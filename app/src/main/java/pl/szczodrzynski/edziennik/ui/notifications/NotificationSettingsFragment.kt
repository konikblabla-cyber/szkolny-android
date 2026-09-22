package pl.szczodrzynski.edziennik.ui.notifications

import android.os.Build
import android.app.AlertDialog
import android.os.Bundle
import android.content.pm.PackageManager
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
        updatePermissionUi()
        if (app.config.sync.lessonNotificationsEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 47002)
        }
        b.automaticSilence.isChecked = app.config.sync.automaticSilenceEnabled
        b.lessonNameNotifications.isChecked = app.config.sync.lessonNameNotifications
        b.planChangeNotifications.isChecked = app.config.sync.planChangeNotifications
        b.systemNotifications.isChecked = app.config.sync.systemNotifications
        b.minutesSeek.progress = app.config.sync.lessonNotificationMinutes.coerceIn(1, 30)
        updateMinutes()

        b.backButton.setOnClickListener {
            activity.onBackPressedDispatcher.onBackPressed()
        }

        b.lessonNotifications.setOnCheckedChangeListener { _, checked ->
            if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 47002)
            }
            app.config.sync.lessonNotificationsEnabled = checked
            if (checked) {
                AximoLessonNotifications.scheduleTodayAndTomorrow(app, app.profile.id)
            } else {
                AximoLessonNotifications.cancelAll(app)
            }
        }

        b.automaticSilence.setOnCheckedChangeListener { _, checked ->
            // School mode primarily uses Android's normal silent ringer mode.
            // ACCESS_NOTIFICATION_POLICY is optional and must never block activation.
            app.config.sync.automaticSilenceEnabled = checked
            if (checked) {
                AximoLessonSilence.scheduleTodayAndTomorrow(app, app.profile.id)
                if (!AximoLessonSilence.hasNotificationPolicyAccess(app)) {
                    android.widget.Toast.makeText(
                        activity,
                        "Tryb szkolny został włączony. Podstawowe wyciszanie działa bez dodatkowego dostępu Androida.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
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

        b.lessonNameNotifications.setOnCheckedChangeListener { _, checked -> app.config.sync.lessonNameNotifications = checked }
        b.planChangeNotifications.setOnCheckedChangeListener { _, checked -> app.config.sync.planChangeNotifications = checked }
        b.systemNotifications.setOnCheckedChangeListener { _, checked -> app.config.sync.systemNotifications = checked }

        b.testSilenceButton.visibility = android.view.View.GONE
        b.testSilenceButton.setOnClickListener {
            if (!AximoLessonSilence.hasNotificationPolicyAccess(app)) {
                AximoLessonSilence.openNotificationPolicyAccessSettings(activity)
                return@setOnClickListener
            }
            val input = android.widget.EditText(activity).apply {
                hint = "Kod developerski"
                inputType = android.text.InputType.TYPE_CLASS_TEXT
            }
            AlertDialog.Builder(activity)
                .setTitle("Test wyciszenia")
                .setMessage("Wpisz kod developerski, aby na 10 sekund wyciszyć telefon.")
                .setView(input)
                .setNegativeButton("Anuluj", null)
                .setPositiveButton("Uruchom") { _, _ ->
                    if (input.text.toString() == "89@#") {
                        val started = AximoLessonSilence.testForDuration(app, 10_000L)
                        android.widget.Toast.makeText(
                            activity,
                            if (started) "Test wyciszenia uruchomiony na 10 sekund." else "Brak dostępu do wyciszania.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        android.widget.Toast.makeText(activity, "Nieprawidłowy kod.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }

        b.filterCard.setOnClickListener {
            NotificationFilterDialog(activity).show()
        }

        b.silencePermissionButton.setOnClickListener {
            AximoLessonSilence.openNotificationPolicyAccessSettings(activity)
        }
    }

    override fun onResume() {
        super.onResume()
        if (view != null) updatePermissionUi()
    }

    private fun updatePermissionUi() {
        val granted = AximoLessonSilence.hasNotificationPolicyAccess(requireContext())
        b.silencePermissionStatus.text = if (granted) {
            "✓ Dostęp przyznany — Aximo może automatycznie wyciszać telefon podczas szkoły."
        } else {
            "✓ Podstawowe wyciszanie działa bez tego dostępu. Opcjonalny dostęp „Nie przeszkadzać” rozszerza tryb szkolny."
        }
        b.silencePermissionButton.text = if (granted) "Ustawienia dodatkowego dostępu" else "Opcjonalny dostęp „Nie przeszkadzać”"
    }

    private fun updateMinutes() {
        b.minutesValue.text =
            "Przypomnienie ${app.config.sync.lessonNotificationMinutes} minut przed rozpoczęciem"
    }
}
