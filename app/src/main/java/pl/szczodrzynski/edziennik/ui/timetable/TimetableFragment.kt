/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.ui.timetable

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.datepicker.MaterialDatePicker
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.szkolny.font.SzkolnyFont
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.databinding.FragmentTimetableV2Binding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.JsonObject
import pl.szczodrzynski.edziennik.ext.getSchoolYearConstrains
import pl.szczodrzynski.edziennik.ext.getStudentData
import pl.szczodrzynski.edziennik.ui.base.fragment.PagerFragment
import pl.szczodrzynski.edziennik.ui.dialogs.settings.TimetableConfigDialog
import pl.szczodrzynski.edziennik.ui.event.EventManualDialog
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Week
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem

class TimetableFragment : PagerFragment<FragmentTimetableV2Binding, MainActivity>(
    inflater = FragmentTimetableV2Binding::inflate,
) {
    companion object {
        private const val TAG = "TimetableFragment"
        const val ACTION_SCROLL_TO_DATE = "pl.szczodrzynski.edziennik.timetable.SCROLL_TO_DATE"
        const val ACTION_RELOAD_PAGES = "pl.szczodrzynski.edziennik.timetable.RELOAD_PAGES"
        const val DEFAULT_START_HOUR = 6
        const val DEFAULT_END_HOUR = 19
        var pageSelection: Date? = null
    }

    override fun getFab() = R.string.timetable_today to SzkolnyFont.Icon.szf_calendar_today_outline
    override fun getMarkAsReadType() = MetadataType.LESSON_CHANGE
    override fun getBottomSheetItems() = listOf(
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_timetable_sync)
            .withIcon(CommunityMaterial.Icon.cmd_calendar_sync_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                val date = pageSelection ?: Date.getToday()
                val weekStart = date.weekStart.stringY_m_d
                EdziennikTask.syncProfile(
                    profileId = App.profileId,
                    featureTypes = setOf(FeatureType.TIMETABLE),
                    arguments = JsonObject(
                        "weekStart" to weekStart
                    )
                ).enqueue(activity)
            },
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.timetable_select_day)
            .withIcon(SzkolnyFont.Icon.szf_calendar_today_outline)
            .withOnClickListener { _ ->
                activity.bottomSheet.close()
                val date = pageSelection ?: Date.getToday()
                MaterialDatePicker.Builder.datePicker()
                    .setSelection(date.inMillisUtc)
                    .setCalendarConstraints(app.profile.getSchoolYearConstrains())
                    .build()
                    .apply {
                        addOnPositiveButtonClickListener { millis ->
                            val dateSelected = Date.fromMillisUtc(millis)
                            val index = items.indexOfFirst { it == dateSelected }
                            if (index != -1)
                                b.viewPager.setCurrentItem(index, true)
                        }
                    }
                    .show(activity.supportFragmentManager, TAG)
            },
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_add_event)
            .withDescription(R.string.menu_add_event_desc)
            .withIcon(SzkolnyFont.Icon.szf_calendar_plus_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                EventManualDialog(
                    activity,
                    App.profileId,
                    defaultDate = items[savedPageSelection]
                ).show()
            },
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_generate_block_timetable)
            .withDescription(R.string.menu_generate_block_timetable_desc)
            .withIcon(CommunityMaterial.Icon3.cmd_table_large)
            .withOnClickListener {
                activity.bottomSheet.close()
                GenerateBlockTimetableDialog(activity)
            },
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_timetable_config)
            .withIcon(CommunityMaterial.Icon.cmd_cog_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                TimetableConfigDialog(activity, false).show()
            }
    )

    override fun getTabLayout() = b.tabLayout
    override fun getViewPager() = b.viewPager

    override fun getPageCount() = items.size
    override fun getPageFragment(position: Int) = TimetableDayFragment().apply {
        arguments = Bundle(
            "date" to items[position].value,
            "startHour" to startHour,
            "endHour" to endHour,
        )
    }

    override fun getPageTitle(position: Int): String {
        val date = items[position]
        var pageTitle = Week.getFullDayName(date.weekDay)
        if (date > weekEnd || date < weekStart) {
            pageTitle += ", ${date.stringDm}"
        }
        return pageTitle
    }

    private var startHour = DEFAULT_START_HOUR
    private var endHour = DEFAULT_END_HOUR
    private val today by lazy { Date.getToday() }
    private val weekStart by lazy { today.weekStart }
    private val weekEnd by lazy { weekStart.clone().stepForward(0, 0, 6) }
    private val items = mutableListOf<Date>()
    private var fabShown = false
    private fun renderAximoPlan(date: Date) {
        if (!isAdded) return

        b.aximoPlanDate.text = "${Week.getFullDayName(date.weekDay)}, ${date.stringDm}"
        b.aximoDayStrip.removeAllViews()

        for (offset in 0..6) {
            val day = weekStart.clone().stepForward(0, 0, offset)
            val selected = day == date
            val chip = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(70.dp, 56.dp).apply { marginEnd = 7.dp }
                setPadding(4.dp, 5.dp, 4.dp, 5.dp)
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 18.dp.toFloat()
                    setColor(if (selected) 0xFF7346D8.toInt() else 0xFF111B35.toInt())
                    setStroke(1.dp, if (selected) 0xFFA982FF.toInt() else 0xFF26345A.toInt())
                }
                setOnClickListener {
                    val index = items.indexOfFirst { it == day }
                    if (index >= 0) b.viewPager.setCurrentItem(index, true)
                }
            }
            val dayName = TextView(requireContext()).apply {
                text = Week.getFullDayName(day.weekDay).take(2).uppercase()
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(if (selected) 0xFFFFFFFF.toInt() else 0xFF8F9DBB.toInt())
            }
            val dayNumber = TextView(requireContext()).apply {
                text = day.day.toString()
                textSize = 17f
                gravity = Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(if (selected) 0xFFFFFFFF.toInt() else 0xFFE8ECF8.toInt())
            }
            chip.addView(dayName, LinearLayout.LayoutParams(-1, 18.dp))
            chip.addView(dayNumber, LinearLayout.LayoutParams(-1, 24.dp))
            b.aximoDayStrip.addView(chip)
        }

        val lessons = try {
            app.db.timetableDao().getAllForDateNow(App.profileId, date)
                .filter { it.type != Lesson.TYPE_CANCELLED && it.type != Lesson.TYPE_NO_LESSONS }
                .sortedBy { it.displayStartTime?.toString() ?: "" }
        } catch (_: Exception) { emptyList() }

        b.aximoLessonContainer.removeAllViews()
        b.aximoPlanCount.text = "${lessons.size} lekcji"
        b.aximoEmptyState.visibility = if (lessons.isEmpty()) View.VISIBLE else View.GONE

        val accentColors = intArrayOf(
            0xFF62D99C.toInt(), 0xFF55A8FF.toInt(), 0xFFE36CFF.toInt(),
            0xFFFFA45B.toInt(), 0xFF46D9E8.toInt(), 0xFF76D46A.toInt()
        )

        lessons.forEachIndexed { index, lesson ->
            val startMillis = runCatching {
                lesson.displayStartTime?.let { date.getAsCalendar(it).timeInMillis }
            }.getOrNull() ?: 0L
            val endMillis = runCatching {
                lesson.displayEndTime?.let { date.getAsCalendar(it).timeInMillis }
            }.getOrNull()?.takeIf { it > startMillis } ?: (startMillis + 45 * 60_000L)
            val now = System.currentTimeMillis()
            val current = now in startMillis until endMillis
            val accent = accentColors[index % accentColors.size]

            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = 88.dp
                setPadding(0, 0, 12.dp, 0)
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 20.dp.toFloat()
                    setColor(if (current) 0xFF171F40.toInt() else 0xFF0D1730.toInt())
                    setStroke(1.dp, if (current) accent else 0xFF1D2B4D.toInt())
                }
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10.dp }
            }

            val stripe = View(requireContext()).apply {
                setBackgroundColor(accent)
                layoutParams = LinearLayout.LayoutParams(5.dp, -1)
            }
            card.addView(stripe)

            val number = TextView(requireContext()).apply {
                text = "${index + 1}"
                gravity = Gravity.CENTER
                textSize = 12f
                setTextColor(0xFFE9EDFA.toInt())
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(0xFF182441.toInt())
                }
                layoutParams = LinearLayout.LayoutParams(32.dp, 32.dp).apply {
                    marginStart = 12.dp
                    marginEnd = 10.dp
                }
            }
            card.addView(number)

            val time = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(58.dp, -2)
                text = "${lesson.displayStartTime ?: "--:--"}\n${lesson.displayEndTime ?: "--:--"}"
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(0xFFB9C4DF.toInt())
            }
            card.addView(time)

            val details = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            val subject = TextView(requireContext()).apply {
                text = lesson.displaySubjectName?.takeIf { it.isNotBlank() } ?: "Lekcja"
                textSize = 16f
                setTextColor(0xFFFFFFFF.toInt())
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            val room = TextView(requireContext()).apply {
                text = lesson.displayClassroom?.takeIf { it.isNotBlank() }?.let { "Sala $it" } ?: "Sala —"
                textSize = 12f
                setTextColor(0xFF8F9DBB.toInt())
                layoutParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = 4.dp }
            }
            details.addView(subject)
            details.addView(room)
            card.addView(details)

            if (current) {
                val badge = TextView(requireContext()).apply {
                    text = "TERAZ"
                    textSize = 9f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setTextColor(0xFFFFFFFF.toInt())
                    setPadding(8.dp, 5.dp, 8.dp, 5.dp)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 12.dp.toFloat()
                        setColor(accent)
                    }
                }
                card.addView(badge)
            } else {
                val chevron = TextView(requireContext()).apply {
                    text = "›"
                    textSize = 24f
                    gravity = Gravity.CENTER
                    setTextColor(0xFF657493.toInt())
                    layoutParams = LinearLayout.LayoutParams(24.dp, 40.dp)
                }
                card.addView(chevron)
            }
            card.setOnClickListener {
                if (!isAdded) return@setOnClickListener
                LessonDetailsDialog(activity = activity, lesson = lesson).show()
            }
            b.aximoLessonContainer.addView(card)
        }
    }
    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, i: Intent) {
            if (!isAdded)
                return
            when (i.action) {
                ACTION_SCROLL_TO_DATE -> {
                    val dateStr = i.extras?.getString("timetableDate", null) ?: return
                    val date = Date.fromY_m_d(dateStr)
                    goToPage(items.indexOf(date))
                }

                ACTION_RELOAD_PAGES -> {
                    b.viewPager.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(
            activity,
            broadcastReceiver,
            IntentFilter(ACTION_SCROLL_TO_DATE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ContextCompat.registerReceiver(
            activity,
            broadcastReceiver,
            IntentFilter(ACTION_RELOAD_PAGES),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onPause() {
        super.onPause()
        activity.unregisterReceiver(broadcastReceiver)
    }

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        if (app.profile.getStudentData("timetableNotPublic", false)) {
            b.timetableLayout.visibility = View.GONE
            b.timetableNotPublicLayout.visibility = View.VISIBLE
            return
        }
        b.timetableLayout.visibility = View.VISIBLE
        b.timetableNotPublicLayout.visibility = View.GONE

        val deferred = async(Dispatchers.Default) {
            items.clear()

            val monthDayCount = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

            val yearStart = app.profile.dateSemester1Start?.clone() ?: today.clone()
            val yearEnd = app.profile.dateYearEnd
            while (yearStart.value <= yearEnd.value) {
                items += yearStart.clone()
                var maxDays = monthDayCount[yearStart.month - 1]
                if (yearStart.month == 2 && yearStart.isLeap)
                    maxDays++
                yearStart.day++
                if (yearStart.day > maxDays) {
                    yearStart.day = 1
                    yearStart.month++
                }
                if (yearStart.month > 12) {
                    yearStart.month = 1
                    yearStart.year++
                }
            }

            val lessonRanges = app.db.lessonRangeDao().getAllNow(App.profileId)
            startHour = lessonRanges.minOfOrNull { it.startTime.hour } ?: DEFAULT_START_HOUR
            endHour = lessonRanges.maxOfOrNull { it.endTime.hour }?.plus(1) ?: DEFAULT_END_HOUR
        }
        deferred.await()
        if (!isAdded)
            return

        val selectedDate = arguments?.getString("timetableDate", "")
            ?.let { if (it.isBlank()) null else Date.fromY_m_d(it) }
        val openTomorrow = arguments?.getBoolean("aximoTomorrow", false) == true
        val requestedDate = selectedDate ?: if (openTomorrow) today.clone().stepForward(0, 0, 1) else today
        savedPageSelection = items.indexOfFirst { it == requestedDate }.takeIf { it >= 0 }
            ?: items.indexOfFirst { it == today }

        super.onViewReady(savedInstanceState)
        renderAximoPlan(items.getOrNull(savedPageSelection) ?: items.firstOrNull() ?: today)
    }

    override suspend fun onFabClick() {
        b.viewPager.setCurrentItem(items.indexOfFirst { it == today }, true)
    }

    override suspend fun onPageSelected(position: Int) {
        renderAximoPlan(items[position])
        activity.navView.bottomBar.fabEnable = items[position] != today
        if (activity.navView.bottomBar.fabEnable && !fabShown) {
            activity.gainAttentionFAB()
            fabShown = true
        }
    }

    /*private fun markLessonsAsSeen() = pageSelection?.let { date ->
        app.db.timetableDao().getForDate(App.profileId, date).observeOnce(this@TimetableFragment, Observer { lessons ->
            lessons.forEach { lesson ->
                if (lesson.type != Lesson.TYPE_NORMAL && lesson.type != Lesson.TYPE_NO_LESSONS
                        && !lesson.seen) {
                    app.db.metadataDao().setSeen(lesson.profileId, lesson, true)
                }
            }
        })
    }*/
}
