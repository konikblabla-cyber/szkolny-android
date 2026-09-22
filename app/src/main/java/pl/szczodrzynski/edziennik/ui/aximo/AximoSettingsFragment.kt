package pl.szczodrzynski.edziennik.ui.aximo

import android.os.Bundle
import android.widget.Toast
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.FragmentAximoSettingsBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.core.aximo.AximoLessonSilence
import pl.szczodrzynski.edziennik.core.aximo.AximoLessonNotifications
import pl.szczodrzynski.edziennik.data.enums.NavTarget

class AximoSettingsFragment : BaseFragment<FragmentAximoSettingsBinding, MainActivity>(
    inflater = FragmentAximoSettingsBinding::inflate,
) {
    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 47002)
        }

        b.backButton.setOnClickListener { activity.onBackPressedDispatcher.onBackPressed() }
        b.profileCard.setOnClickListener { activity.navigate(navTarget = NavTarget.PROFILE_MANAGER) }
        b.notificationsCard.setOnClickListener { activity.navigate(navTarget = NavTarget.NOTIFICATION_SETTINGS) }
        b.silenceCard.setOnClickListener { activity.navigate(navTarget = NavTarget.SILENCE) }
        b.appearanceCard.setOnClickListener { activity.navigate(navTarget = NavTarget.APPEARANCE) }
        b.layoutCard.setOnClickListener { activity.navigate(navTarget = NavTarget.MORE) }
        b.helpCard.setOnClickListener { activity.navigate(navTarget = NavTarget.HELP) }
        b.aboutCard.setOnClickListener { activity.navigate(navTarget = NavTarget.ABOUT) }
        b.notificationPermissionCard.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 47002)
            } else {
                Toast.makeText(activity, "Powiadomienia są już dostępne.", Toast.LENGTH_SHORT).show()
            }
        }
        b.exactAlarmCard.setOnClickListener {
            if (AximoLessonNotifications.canScheduleExactAlarms(requireContext())) {
                Toast.makeText(activity, "Precyzyjne przypomnienia są już dostępne.", Toast.LENGTH_SHORT).show()
            } else {
                AximoLessonNotifications.openExactAlarmSettings(activity)
            }
        }
        b.silencePermissionCard.setOnClickListener {
            if (AximoLessonSilence.hasNotificationPolicyAccess(requireContext())) {
                Toast.makeText(activity, "Dodatkowy dostęp „Nie przeszkadzać” jest już przyznany.", Toast.LENGTH_SHORT).show()
            } else {
                AximoLessonSilence.openNotificationPolicyAccessSettings(activity)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (view != null) { updatePermission(); updateNotificationPermission(); updateExactAlarmPermission() }
    }

    private fun updateNotificationPermission() {
        val granted = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
            requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        b.notificationPermissionValue.text = if (granted) "Przyznane • przypomnienia mogą pojawiać się na ekranie" else "Wymagane • dotknij, aby zezwolić Androidowi"
        b.notificationPermissionValue.setTextColor(requireContext().getColor(if (granted) R.color.aximo_success else R.color.aximo_muted))
    }

    private fun updateExactAlarmPermission() {
        val granted = AximoLessonNotifications.canScheduleExactAlarms(requireContext())
        b.exactAlarmValue.text = if (granted) "Dostępny • przypomnienia mogą być punktualne" else "Wymagany • dotknij, aby zezwolić Androidowi"
        b.exactAlarmValue.setTextColor(requireContext().getColor(if (granted) R.color.aximo_success else R.color.aximo_muted))
    }

    private fun updatePermission() {
        val granted = AximoLessonSilence.hasNotificationPolicyAccess(requireContext())
        b.silencePermissionValue.text = if (granted)
            "Przyznany • tryb szkolny może działać automatycznie"
        else
            "Opcjonalny • zwykłe wyciszanie telefonu działa bez tego"
        b.silencePermissionValue.setTextColor(
            requireContext().getColor(if (granted) R.color.aximo_success else R.color.aximo_muted)
        )
    }
}
