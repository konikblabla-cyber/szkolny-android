package pl.szczodrzynski.edziennik.ui.aximo

import android.os.Bundle
import android.graphics.Color
import android.content.Intent
import android.provider.Settings
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.core.aximo.AximoLessonSilence
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.databinding.FragmentAximoSilenceBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment

class AximoSilenceFragment : BaseFragment<FragmentAximoSilenceBinding, MainActivity>(
    inflater = FragmentAximoSilenceBinding::inflate,
) {
    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        b.schoolModeSwitch.isChecked = app.config.sync.automaticSilenceEnabled
        updateStatus()
        b.backButton.setOnClickListener { activity.onBackPressedDispatcher.onBackPressed() }
        b.schoolModeSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked && !AximoLessonSilence.hasNotificationPolicyAccess(app)) {
                b.schoolModeSwitch.isChecked = false
                app.config.sync.automaticSilenceEnabled = false
                AximoLessonSilence.openNotificationPolicyAccessSettings(activity)
                updateStatus()
                return@setOnCheckedChangeListener
            }
            app.config.sync.automaticSilenceEnabled = checked
            if (checked) AximoLessonSilence.scheduleTodayAndTomorrow(app, app.profile.id)
            else {
                AximoLessonSilence.scheduleTodayAndTomorrow(app, app.profile.id)
                AximoLessonSilence.disableAndRestore(app)
            }
            updateStatus()
        }
        b.settingsButton.setOnClickListener {
            if (!AximoLessonSilence.hasNotificationPolicyAccess(app)) {
                AximoLessonSilence.openNotificationPolicyAccessSettings(activity)
            } else {
                activity.navigate(navTarget = NavTarget.NOTIFICATION_SETTINGS)
            }
        }
    }

    private fun updateStatus() {
        val enabled = app.config.sync.automaticSilenceEnabled
        b.statusText.text = if (enabled) "● Aktywny" else "● Wyłączony"
        b.statusText.setTextColor(Color.parseColor(if (enabled) "#62E59A" else "#8792B0"))
    }
}