package pl.szczodrzynski.edziennik.ui.notifications

import android.os.Bundle
import android.widget.SeekBar
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.databinding.NotificationSettingsFragmentBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.ui.dialogs.settings.NotificationFilterDialog
import pl.szczodrzynski.edziennik.core.aximo.AximoLessonNotifications
import pl.szczodrzynski.edziennik.core.aximo.AximoLessonSilence

class NotificationSettingsFragment : BaseFragment<NotificationSettingsFragmentBinding, MainActivity>(
    inflater = NotificationSettingsFragmentBinding::inflate,
) {
    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        b.lessonNotifications.isChecked = app.config.sync.lessonNotificationsEnabled
        b.automaticSilence.isChecked = app.config.sync.automaticSilenceEnabled
        b.minutesSeek.progress = app.config.sync.lessonNotificationMinutes.coerceIn(1, 30)
        updateMinutes()

        b.backButton.setOnClickListener {
            activity.onBackPressedDispatcher.onBackPressed()
        }

        b.lessonNotifications.setOnCheckedChangeListener { _, checked ->
            app.config.sync.lessonNotificationsEnabled = checked
            if (checked) {
                AximoLessonNotifications.scheduleTodayAndTomorrow(app, app.profile.id)
            } else {
                AximoLessonNotifications.scheduleTodayAndTomorrow(app, app.profile.id)
            }
        }

        b.automaticSilence.setOnCheckedChangeListener { _, checked ->
            app.config.sync.automaticSilenceEnabled = checked
            if (checked) {
                AximoLessonSilence.scheduleTodayAndTomorrow(app, app.profile.id)
            } else {
                AximoLessonSilence.scheduleTodayAndTomorrow(app, app.profile.id)
                AximoLessonSilence.disableAndRestore(app)
            }
        }

        b.minutesSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean,
            ) {
                if (fromUser) {
                    app.config.sync.lessonNotificationMinutes = progress.coerceAtLeast(1)
                    updateMinutes()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                AximoLessonNotifications.scheduleTodayAndTomorrow(app, app.profile.id)
            }
        })

        b.filterCard.setOnClickListener {
            NotificationFilterDialog(activity).show()
        }
    }

    private fun updateMinutes() {
        b.minutesValue.text =
            "Przypomnienie ${app.config.sync.lessonNotificationMinutes} minut przed rozpoczęciem"
    }
}
