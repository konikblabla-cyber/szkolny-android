package pl.szczodrzynski.edziennik.ui.aximo

import android.os.Bundle
import android.widget.Toast
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.FragmentAximoSettingsBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.core.aximo.AximoLessonSilence
import pl.szczodrzynski.edziennik.data.enums.NavTarget

class AximoSettingsFragment : BaseFragment<FragmentAximoSettingsBinding, MainActivity>(
    inflater = FragmentAximoSettingsBinding::inflate,
) {
    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        b.backButton.setOnClickListener { activity.onBackPressedDispatcher.onBackPressed() }
        b.profileCard.setOnClickListener { activity.navigate(navTarget = NavTarget.PROFILE_MANAGER) }
        b.notificationsCard.setOnClickListener { activity.navigate(navTarget = NavTarget.NOTIFICATION_SETTINGS) }
        b.silenceCard.setOnClickListener { activity.navigate(navTarget = NavTarget.SILENCE) }
        b.appearanceCard.setOnClickListener { activity.navigate(navTarget = NavTarget.APPEARANCE) }
        b.helpCard.setOnClickListener { activity.navigate(navTarget = NavTarget.HELP) }
        b.aboutCard.setOnClickListener { activity.navigate(navTarget = NavTarget.ABOUT) }
        b.silencePermissionCard.setOnClickListener {
            if (AximoLessonSilence.hasNotificationPolicyAccess(requireContext())) {
                Toast.makeText(activity, "Dostęp do trybu Nie przeszkadzać jest już przyznany.", Toast.LENGTH_SHORT).show()
            } else {
                AximoLessonSilence.openNotificationPolicyAccessSettings(activity)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (view != null) updatePermission()
    }

    private fun updatePermission() {
        val granted = AximoLessonSilence.hasNotificationPolicyAccess(requireContext())
        b.silencePermissionValue.text = if (granted)
            "Przyznany • tryb szkolny może działać automatycznie"
        else
            "Wymagany • dotknij, aby nadać dostęp w Androidzie"
        b.silencePermissionValue.setTextColor(
            requireContext().getColor(if (granted) R.color.aximo_success else R.color.aximo_muted)
        )
    }
}
