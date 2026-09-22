package pl.szczodrzynski.edziennik.ui.settings

import android.os.Bundle
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AximoSettingsFragmentBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.core.aximo.AximoLessonSilence

class SettingsFragment : BaseFragment<AximoSettingsFragmentBinding, MainActivity>(
    inflater = AximoSettingsFragmentBinding::inflate,
) {
    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        b.backButton.setOnClickListener { activity.onBackPressedDispatcher.onBackPressed() }
        b.profileButton.setOnClickListener { activity.navigate(navTarget = NavTarget.PROFILE_MANAGER) }
        b.notificationSettingsButton.setOnClickListener { activity.navigate(navTarget = NavTarget.NOTIFICATION_SETTINGS) }
        b.silenceButton.setOnClickListener { activity.navigate(navTarget = NavTarget.SILENCE) }
        b.appearanceButton.setOnClickListener { activity.navigate(navTarget = NavTarget.APPEARANCE) }
        b.moreButton.setOnClickListener { activity.navigate(navTarget = NavTarget.MORE) }
        b.helpButton.setOnClickListener { activity.navigate(navTarget = NavTarget.HELP) }
        b.aboutButton.setOnClickListener { activity.navigate(navTarget = NavTarget.ABOUT) }

        b.schoolModeSwitch.isChecked = app.config.sync.automaticSilenceEnabled
        b.schoolModeSwitch.setOnCheckedChangeListener { _, checked ->
            app.config.sync.automaticSilenceEnabled = checked
            if (checked) {
                AximoLessonSilence.scheduleTodayAndTomorrow(app, app.profile.id)
            } else {
                AximoLessonSilence.disableAndRestore(app)
            }
        }

        b.profileSummary.text = try {
            val profile = app.profile
            if (profile.name.isNotBlank()) "${profile.name} • ${profile.subname}"
            else getString(R.string.aximo_settings_school_subtitle)
        } catch (_: Exception) {
            getString(R.string.aximo_settings_school_subtitle)
        }
    }
}