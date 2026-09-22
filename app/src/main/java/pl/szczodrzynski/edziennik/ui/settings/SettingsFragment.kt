package pl.szczodrzynski.edziennik.ui.settings

import android.os.Bundle
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AximoSettingsFragmentBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.core.aximo.AximoLessonSilence
import pl.szczodrzynski.edziennik.core.aximo.AximoLessonNotifications

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
        b.permissionButton.setOnClickListener {
            AximoLessonSilence.openNotificationPolicyAccessSettings(activity)
        }
        updatePermissionUi()
        b.aboutButton.setOnClickListener { activity.navigate(navTarget = NavTarget.ABOUT) }
        b.developerButton.setOnClickListener { showDeveloperPanel() }

        val silenceAccess = AximoLessonSilence.hasNotificationPolicyAccess(app)
        b.schoolModeSwitch.isChecked = app.config.sync.automaticSilenceEnabled && silenceAccess
        b.schoolModeSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked && !AximoLessonSilence.hasNotificationPolicyAccess(app)) {
                b.schoolModeSwitch.isChecked = false
                app.config.sync.automaticSilenceEnabled = false
                AximoLessonSilence.openNotificationPolicyAccessSettings(activity)
                return@setOnCheckedChangeListener
            }
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

    private fun showDeveloperPanel() {
        val dnd = AximoLessonSilence.hasNotificationPolicyAccess(activity)
        val post = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val exact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (activity.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager).canScheduleExactAlarms()
        } else true

        val status = "DND: " + if (dnd) "✓ OK" else "✕ brak" +
            " • Powiadomienia: " + if (post) "✓ OK" else "✕ brak" +
            " • Alarmy: " + if (exact) "✓ OK" else "⚠ przybliżone"

        val items = arrayOf(
            "🔎 Sprawdź uprawnienia",
            "🔕 Wycisz telefon na 20 sekund",
            "🔊 Przywróć dźwięk teraz",
            "🔔 Wyślij testowe powiadomienie",
            "⏱ Odśwież alarmy lekcji",
            "🌙 Odśwież tryb szkolny",
            "⚙ Otwórz ustawienia DND"
        )

        AlertDialog.Builder(activity)
            .setTitle("Panel developerski")
            .setMessage(status)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> Toast.makeText(activity, status, Toast.LENGTH_LONG).show()
                    1 -> {
                        if (!AximoLessonSilence.hasNotificationPolicyAccess(activity)) {
                            AximoLessonSilence.openNotificationPolicyAccessSettings(activity)
                        } else {
                            val ok = AximoLessonSilence.testForDuration(activity, 20_000L)
                            Toast.makeText(activity, if (ok) "Telefon wyciszony na 20 sekund." else "Nie udało się uruchomić testu.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    2 -> {
                        AximoLessonSilence.disableAndRestore(activity)
                        Toast.makeText(activity, "Przywrócono dźwięk.", Toast.LENGTH_SHORT).show()
                    }
                    3 -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 47003)
                            Toast.makeText(activity, "Nadaj dostęp do powiadomień i uruchom test ponownie.", Toast.LENGTH_SHORT).show()
                        } else {
                            AximoLessonNotifications.testNotification(activity)
                            Toast.makeText(activity, "Wysłano testowe powiadomienie.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    4 -> {
                        AximoLessonNotifications.scheduleTodayAndTomorrow(activity, app.profile.id)
                        Toast.makeText(activity, "Alarmy lekcji odświeżone.", Toast.LENGTH_SHORT).show()
                    }
                    5 -> {
                        AximoLessonSilence.scheduleTodayAndTomorrow(activity, app.profile.id)
                        Toast.makeText(activity, "Tryb szkolny odświeżony.", Toast.LENGTH_SHORT).show()
                    }
                    6 -> AximoLessonSilence.openNotificationPolicyAccessSettings(activity)
                }
            }
            .setNegativeButton("Zamknij", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (view != null) updatePermissionUi()
    }

    private fun updatePermissionUi() {
        val granted = AximoLessonSilence.hasNotificationPolicyAccess(requireContext())
        b.permissionStatus.text = if (granted) {
            "✓ Dostęp przyznany — Aximo może automatycznie wyciszać telefon w czasie szkoły."
        } else {
            "⚠ Brak dostępu. Android nie pokazuje tu zwykłego okna uprawnień — trzeba włączyć dostęp w ustawieniach systemu."
        }
        b.permissionButton.text = if (granted) "Otwórz ustawienia dostępu" else "Otwórz dostęp „Nie przeszkadzać”"
        b.permissionButton.isEnabled = true
        b.schoolModeSwitch.isEnabled = granted
        if (!granted && app.config.sync.automaticSilenceEnabled) {
            app.config.sync.automaticSilenceEnabled = false
            b.schoolModeSwitch.isChecked = false
        }
    }
}