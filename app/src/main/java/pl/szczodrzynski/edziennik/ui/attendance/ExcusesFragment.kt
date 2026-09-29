package pl.szczodrzynski.edziennik.ui.attendance

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.databinding.AttendanceListFragmentBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment

class ExcusesFragment : BaseFragment<AttendanceListFragmentBinding, MainActivity>(
    inflater = AttendanceListFragmentBinding::inflate,
) {
    override fun getScrollingView() = b.list

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        val adapter = AttendanceAdapter(activity, AttendanceFragment.VIEW_LIST)

        app.db.attendanceDao().getAll(App.profileId).observe(this@ExcusesFragment, Observer { all ->
            if (!isAdded) return@Observer

            val items = all
                .filter {
                    it.baseType == Attendance.TYPE_ABSENT ||
                    it.baseType == Attendance.TYPE_BELATED
                }
                .sortedWith(
                    compareByDescending<AttendanceFull> { it.date.year }
                        .thenByDescending { it.date.month }
                        .thenByDescending { it.date.day }
                )

            adapter.items = items.toMutableList()
            if (b.list.adapter == null) {
                b.list.adapter = adapter
                b.list.layoutManager = LinearLayoutManager(context)
                b.list.setHasFixedSize(true)
            }
            adapter.notifyDataSetChanged()

            b.progressBar.isVisible = false
            b.list.isVisible = items.isNotEmpty()
            b.noData.isVisible = items.isEmpty()
        })

        adapter.onAttendanceClick = {
            AttendanceDetailsDialog(activity, it).show()
        }
    }
}
