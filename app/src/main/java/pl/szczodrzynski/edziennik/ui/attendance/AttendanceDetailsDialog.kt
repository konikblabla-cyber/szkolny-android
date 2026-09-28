/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-9.
 */

package pl.szczodrzynski.edziennik.ui.attendance

import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.databinding.AttendanceDetailsDialogBinding
import pl.szczodrzynski.edziennik.ext.setTintColor
import pl.szczodrzynski.edziennik.ui.base.dialog.BindingDialog
import pl.szczodrzynski.edziennik.ui.notes.setupNotesButton
import pl.szczodrzynski.edziennik.utils.BetterLink
import pl.szczodrzynski.edziennik.core.manager.NoteManager

class AttendanceDetailsDialog(
    activity: AppCompatActivity,
    private val attendance: AttendanceFull,
    private val showNotes: Boolean = true,
) : BindingDialog<AttendanceDetailsDialogBinding>(activity) {

    override fun getTitleRes(): Int? = null
    override fun inflate(layoutInflater: LayoutInflater) =
        AttendanceDetailsDialogBinding.inflate(layoutInflater)

    override fun getPositiveButtonText() = R.string.close

    override suspend fun onBeforeShow(): Boolean {
        val manager = app.attendanceManager

        val attendanceColor = manager.getAttendanceColor(attendance)
        b.attendance = attendance
        b.devMode = App.devMode
        b.attendanceName.setTextColor(if (ColorUtils.calculateLuminance(attendanceColor) > 0.3) 0xaa000000.toInt() else 0xccffffff.toInt())
        b.attendanceName.background.setTintColor(attendanceColor)

        b.attendanceIsCounted.setText(if (attendance.isCounted) R.string.yes else R.string.no)

        attendance.teacherName?.let { name ->
            BetterLink.attach(
                b.teacherName,
                teachers = mapOf(attendance.teacherId to name),
                onActionSelected = ::dismiss
            )
        }

        val canRequestExcuse = attendance.baseType == Attendance.TYPE_ABSENT ||
                attendance.baseType == Attendance.TYPE_BELATED

        b.excuseButton.isVisible = canRequestExcuse
        b.excuseButton.setOnClickListener {
            val teacher = app.db.teacherDao().getByIdNow(attendance.profileId, attendance.teacherId)
            if (teacher?.loginId == null) {
                activity.snackbar("Nie znaleziono odbiorcy dla usprawiedliwienia.")
                return@setOnClickListener
            }
            val input = EditText(activity).apply {
                hint = "Np. wizyta u lekarza, choroba..."
                minLines = 3
                setPadding(24, 16, 24, 16)
            }
            AlertDialog.Builder(activity)
                .setTitle("Usprawiedliwienie nieobecności")
                .setMessage("Lekcja: " + attendance.subjectLongName + "\\nData: " + attendance.date.formattedString)
                .setView(input)
                .setPositiveButton("Wyślij") { _, _ ->
                    val reason = input.text.toString().trim()
                    if (reason.length < 3) {
                        activity.snackbar("Podaj powód usprawiedliwienia.")
                        return@setPositiveButton
                    }
                    val isLate = attendance.baseType == Attendance.TYPE_BELATED
                    val subject = if (isLate) "Prośba o usprawiedliwienie spóźnienia" else "Prośba o usprawiedliwienie nieobecności"
                    val body = "Proszę o usprawiedliwienie " + (if (isLate) "spóźnienia" else "nieobecności") +
                            " z dnia " + attendance.date.formattedString + " z przedmiotu " + attendance.subjectLongName + ".\\n\\nPowód: " + reason
                    EdziennikTask.messageSend(App.profileId, setOf(teacher), subject, body).enqueue(activity)
                    activity.snackbar("Wysłano prośbę o usprawiedliwienie.")
                }
                .setNegativeButton("Anuluj", null)
                .show()
        }
        b.notesButton.isVisible = showNotes
        b.notesButton.setupNotesButton(
            activity = activity,
            owner = attendance,
        )
        b.legend.isVisible = showNotes
        if (showNotes)
            NoteManager.setLegendText(attendance, b.legend)
        return true
    }
}
