package pl.szczodrzynski.edziennik.ui.grades

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.full.GradeFull
import pl.szczodrzynski.edziennik.databinding.GradesStatisticsFragmentBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GradesStatisticsFragment : BaseFragment<GradesStatisticsFragmentBinding, MainActivity>(
    inflater = GradesStatisticsFragmentBinding::inflate,
) {
    private var allGrades: List<GradeFull> = emptyList()
    private var selectedSubjectId = 0L
    private var range = RANGE_SCHOOL_YEAR

    private data class MonthStats(
        val year: Int,
        val month: Int,
        val grades: Map<Int, List<String>>,
        val average: Float,
    ) {
        val key: String get() = "%04d-%02d".format(year, month)
        val title: String get() = SimpleDateFormat("MMM yyyy", Locale("pl")).format(
            Calendar.getInstance().apply { set(year, month - 1, 1) }.time
        ).replaceFirstChar { it.uppercase() }
    }

    companion object {
        private const val RANGE_LAST_3 = 0
        private const val RANGE_SEMESTER = 1
        private const val RANGE_SCHOOL_YEAR = 2
    }

    override fun getScrollingView() = b.scrollView

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        setupFilters()

        app.db.gradeDao().getAllOrderBy(
            App.profileId,
            app.gradesManager.getOrderByString()
        ).observe(viewLifecycleOwner, Observer { grades ->
            if (!isAdded) return@Observer
            allGrades = grades.filter {
                it.type == Grade.TYPE_NORMAL && it.value in 1f..6f
            }
            rebuildSubjectFilter()
            render()
        })
    }

    private fun setupFilters() {
        b.rangeGroup.check(R.id.rangeSchoolYear)
        b.rangeGroup.addOnButtonCheckedListener { _, checkedId, checked ->
            if (!checked) return@addOnButtonCheckedListener
            range = when (checkedId) {
                R.id.rangeLast3 -> RANGE_LAST_3
                R.id.rangeSemester -> RANGE_SEMESTER
                else -> RANGE_SCHOOL_YEAR
            }
            render()
        }

        b.subjectDropdown.setOnItemClickListener { _, _, position, _ ->
            val item = b.subjectDropdown.adapter?.getItem(position) as? SubjectItem ?: return@setOnItemClickListener
            selectedSubjectId = item.id
            render()
        }
    }

    private data class SubjectItem(val id: Long, val label: String) {
        override fun toString() = label
    }

    private fun rebuildSubjectFilter() {
        val subjects = mutableListOf(SubjectItem(0L, "Wszystkie przedmioty"))
        allGrades.groupBy { it.subjectId }
            .toList()
            .sortedBy { it.second.firstOrNull()?.subjectLongName?.lowercase() ?: "" }
            .forEach { (id, grades) ->
                subjects += SubjectItem(id, grades.firstOrNull()?.subjectLongName ?: "Przedmiot")
            }

        b.subjectDropdown.setAdapter(
            ArrayAdapter(activity, android.R.layout.simple_dropdown_item_1line, subjects)
        )
        val selected = subjects.firstOrNull { it.id == selectedSubjectId } ?: subjects.first()
        selectedSubjectId = selected.id
        b.subjectDropdown.setText(selected.label, false)
    }

    private fun render() {
        val filtered = filterByRange(allGrades).filter {
            selectedSubjectId == 0L || it.subjectId == selectedSubjectId
        }

        b.noData.isVisible = filtered.isEmpty()
        b.chartScroll.isVisible = filtered.isNotEmpty()
        b.trendView.isVisible = filtered.isNotEmpty()

        if (filtered.isEmpty()) {
            b.chartContainer.removeAllViews()
            b.summaryText.text = "Brak ocen w wybranym zakresie."
            return
        }

        val months = filtered.groupBy { monthKey(it.addedDate) }
            .toSortedMap()
            .map { (key, grades) ->
                val parts = key.split("-")
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val byGrade = (1..6).associateWith { value ->
                    grades.filter { app.gradesManager.getGradeValue(it).toInt().coerceIn(1, 6) == value }
                        .map { it.name }
                }
                MonthStats(year, month, byGrade, grades.map { app.gradesManager.getGradeValue(it) }.average().toFloat())
            }

        buildTable(months)
        b.trendView.setValues(months.map { it.average }, months.map { it.title })
        val avg = filtered.map { app.gradesManager.getGradeValue(it) }.average()
        val gradeText = filtered
            .map { it.name }
            .sortedWith(compareByDescending<String> { it.firstOrNull()?.digitToIntOrNull() ?: 0 })
            .joinToString(", ")
        val distribution = (6 downTo 1).joinToString("   ") { grade ->
            "$grade: ${filtered.count { app.gradesManager.getGradeValue(it).toInt().coerceIn(1, 6) == grade }}"
        }
        b.summaryText.text = "Twoja średnia: ${DecimalFormat("0.00").format(avg)}\\nOceny: $gradeText\\n$distribution"
    }

    private fun buildTable(months: List<MonthStats>) {
        b.chartContainer.removeAllViews()
        val row = android.widget.LinearLayout(activity).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.BOTTOM
            setPadding(16, 16, 16, 16)
        }
        months.forEachIndexed { index, month ->
            val maxBarHeight = (260 * resources.displayMetrics.density).toInt()
            val minBarHeight = (30 * resources.displayMetrics.density).toInt()
            val barHeight = (maxBarHeight * ((month.average - 1f) / 5f)).toInt().coerceIn(minBarHeight, maxBarHeight)
            val grades = month.grades.values.flatten().sortedWith(compareByDescending<String> { it.firstOrNull()?.digitToIntOrNull() ?: 0 }.thenBy { it })
            val best = months.maxByOrNull { it.average }
            val column = android.widget.LinearLayout(activity).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                setPadding(8, 0, 8, 0)
                setOnClickListener { showMonth(month, allGrades.filter { monthKey(it.addedDate) == month.key }) }
                setOnLongClickListener { showMonth(month, allGrades.filter { monthKey(it.addedDate) == month.key }); true }
            }
            column.addView(TextView(activity).apply {
                text = DecimalFormat("0.00").format(month.average) + if (best?.key == month.key) "  ★" else ""
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 0, 0, 6)
            })
            column.addView(TextView(activity).apply {
                text = grades.joinToString(", ")
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setPadding(10, 10, 10, 10)
                minHeight = barHeight
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 22f
                    setColor(com.google.android.material.color.MaterialColors.getColor(this@GradesStatisticsFragment.requireView(), com.google.android.material.R.attr.colorPrimaryContainer))
                }
            })
            column.addView(TextView(activity).apply {
                text = month.title
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 8, 0, 0)
            })
            column.alpha = 0f
            column.animate().alpha(1f).setDuration(250L + index * 60L).start()
            row.addView(column, android.widget.LinearLayout.LayoutParams((128 * resources.displayMetrics.density).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        b.chartContainer.addView(row)
    }
    private fun tableLp(widthDp: Int): android.widget.TableRow.LayoutParams =
        android.widget.TableRow.LayoutParams(
            (widthDp * resources.displayMetrics.density).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

    private fun showMonth(month: MonthStats, source: List<GradeFull>) {
        val values = source.sortedBy { app.gradesManager.getGradeValue(it) }
            .joinToString(", ") { it.name }
        val average = source.map { app.gradesManager.getGradeValue(it) }.average()
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle(month.title)
            .setMessage(
                "Oceny: ${if (values.isBlank()) "brak" else values}\\n" +
                    "Liczba ocen: §{source.size}\\n" +
                    "Średnia miesięczna: ${if (source.isEmpty()) "—" else DecimalFormat("0.00").format(average)}"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun filterByRange(source: List<GradeFull>): List<GradeFull> {
        if (source.isEmpty()) return emptyList()
        val now = Calendar.getInstance()
        val start = Calendar.getInstance()

        when (range) {
            RANGE_LAST_3 -> {
                start.timeInMillis = now.timeInMillis
                start.add(Calendar.MONTH, -2)
                start.set(Calendar.DAY_OF_MONTH, 1)
            }
            RANGE_SEMESTER -> {
                val month = now.get(Calendar.MONTH) + 1
                start.set(Calendar.MONTH, if (month <= 1 || month >= 9) Calendar.SEPTEMBER else Calendar.FEBRUARY)
                if (month <= 1) start.add(Calendar.YEAR, -1)
                start.set(Calendar.DAY_OF_MONTH, 1)
            }
            else -> {
                val month = now.get(Calendar.MONTH) + 1
                start.set(Calendar.MONTH, Calendar.AUGUST)
                if (month < 8) start.add(Calendar.YEAR, -1)
                start.set(Calendar.DAY_OF_MONTH, 1)
            }
        }
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        return source.filter { it.addedDate >= start.timeInMillis }
    }

    private fun monthKey(time: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = time }
        return "%04d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
    }
}
