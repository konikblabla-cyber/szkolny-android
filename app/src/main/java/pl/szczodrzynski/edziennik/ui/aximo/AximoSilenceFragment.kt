package pl.szczodrzynski.edziennik.ui.aximo

import android.os.Bundle
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
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
            app.config.sync.automaticSilenceEnabled = checked
            if (checked) AximoLessonSilence.scheduleTodayAndTomorrow(app, app.profile.id)
            else {
                AximoLessonSilence.scheduleTodayAndTomorrow(app, app.profile.id)
                AximoLessonSilence.disableAndRestore(app)
            }
            updateStatus()
        }
        b.settingsButton.setOnClickListener { activity.navigate(navTarget = NavTarget.NOTIFICATION_SETTINGS) }
    }

    private fun updateStatus() {
        val enabled = app.config.sync.automaticSilenceEnabled
        b.statusText.text = if (enabled) "● Aktywny" else "● Wyłączony"
        b.statusText.setTextColor(requireContext().getColor(if (enabled) R.color.aximo_success else R.color.aximo_muted))
    }
}