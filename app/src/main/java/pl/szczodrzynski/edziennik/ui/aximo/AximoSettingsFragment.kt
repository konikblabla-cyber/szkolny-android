package pl.szczodrzynski.edziennik.ui.aximo

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.FragmentAximoSettingsBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.core.aximo.AximoLessonSilence
import pl.szczodrzynski.edziennik.core.aximo.AximoLessonNotifications
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AximoSettingsFragment : BaseFragment<FragmentAximoSettingsBinding, MainActivity>(
    inflater = FragmentAximoSettingsBinding::inflate,
) {
    private val prefs by lazy { requireContext().getSharedPreferences("aximo_settings", 0) }

    private data class Category(
        val title: String,
        val description: String,
        val container: LinearLayout
    )

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        b.backButton.setOnClickListener { activity.onBackPressedDispatcher.onBackPressed() }

        val categories = listOf(
            Category("Start", "Wybierz tylko to, co naprawdę chcesz widzieć na ekranie głównym.", b.categoryHome),
            Category("Nawigacja", "Ustaw, które sekcje mają być dostępne od ręki.", b.categoryNavigation),
            Category("Dziennik", "Steruj odświeżaniem i zachowaniem planu lekcji.", b.categoryDiary),
            Category("Powiadomienia", "Ustaw informacje o lekcjach i dostęp Androida.", b.categoryNotifications),
            Category("Tryb szkolny", "Automatyczne wyciszanie telefonu podczas lekcji.", b.categorySchool),
            Category("Wygląd", "Motywy, tło, kolory, karty i animacje.", b.categoryPersonal),
            Category("Konto i pomoc", "Konto, synchronizacja, pomoc i informacje o Aximo.", b.categoryAdvanced)
        )

        b.settingsPlan.setOnClickListener { activity.navigate(navTarget = NavTarget.TIMETABLE) }
        b.settingsGrades.setOnClickListener { activity.navigate(navTarget = NavTarget.GRADES) }
        b.settingsAttendance.setOnClickListener { activity.navigate(navTarget = NavTarget.ATTENDANCE) }
        b.settingsMessages.setOnClickListener { activity.navigate(navTarget = NavTarget.MESSAGES) }
        b.settingsHomework.setOnClickListener { activity.navigate(navTarget = NavTarget.HOMEWORK) }
        b.settingsAppearance.setOnClickListener { activity.navigate(navTarget = NavTarget.APPEARANCE) }

        val tabBar = b.settingsTabs
        categories.forEachIndexed { index, category ->
            val tab = TextView(requireContext()).apply {
                text = category.title
                textSize = 13f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(dp(16), 0, dp(16), 0)
                background = tabBackground(index == 0)
                setOnClickListener { showCategory(categories, index) }
            }
            tabBar.addView(tab, LinearLayout.LayoutParams(-2, dp(40)).apply {
                marginStart = if (index == 0) 0 else dp(7)
            })
        }

        addHomeSettings(b.categoryHome)
        addNavigationSettings(b.categoryNavigation)
        addDiarySettings(b.categoryDiary)
        addNotificationSettings(b.categoryNotifications)
        addSchoolSettings(b.categorySchool)
        addAppearanceSettings(b.categoryPersonal)
        addAdvancedSettings(b.categoryAdvanced)

        showCategory(categories, 0)
    }

    private fun addHomeSettings(c: LinearLayout) {
        addSwitch(c, "Karta planu lekcji", "Pokazuj dzisiejszy plan na ekranie głównym.", "home_timetable", true)
        addSwitch(c, "Karta ocen", "Pokazuj najważniejsze oceny i średnią.", "home_grades", true)
        addSwitch(c, "Karta frekwencji", "Pokazuj procent obecności.", "home_attendance", true)
        addSwitch(c, "Karta zadań", "Pokazuj najbliższe prace domowe.", "home_homework", true)
        addSwitch(c, "Karta wiadomości", "Pokazuj najnowsze wiadomości.", "home_messages", true)
        addSwitch(c, "Powitanie i data", "Pokazuj dzień tygodnia oraz powitanie.", "home_greeting", true)
    }

    private fun addNavigationSettings(c: LinearLayout) {
        addSwitch(c, "Menu radialne", "Przytrzymaj dolny przycisk, aby otworzyć szybkie menu.", "nav_radial", true)
        addSwitch(c, "Oceny w menu", "Pokazuj Oceny w menu radialnym.", "nav_grades", true)
        addSwitch(c, "Frekwencja w menu", "Pokazuj Frekwencję w menu radialnym.", "nav_attendance", true)
        addSwitch(c, "Wiadomości w menu", "Pokazuj Wiadomości w menu radialnym.", "nav_messages", true)
        addSwitch(c, "Zadania w menu", "Pokazuj Zadania w menu radialnym.", "nav_homework", true)
    }

    private fun addDiarySettings(c: LinearLayout) {
        addSwitch(c, "Odświeżanie gestem", "Przeciągnięcie w dół odświeża dane.", "diary_swipe_refresh", true)
    }

    private fun addNotificationSettings(c: LinearLayout) {
        addSwitch(c, "Powiadomienie o następnej lekcji", "Pokazuj aktualną i następną lekcję.", "notify_next_lesson", true)
        addChoiceAction(c, "Kiedy przypominać o lekcji", "Ustaw, ile minut przed lekcją ma pojawić się powiadomienie.", "notify_minutes", intArrayOf(1, 5, 10, 15, 20, 30), 10) { value ->
            val app = requireContext().applicationContext as pl.szczodrzynski.edziennik.App
            app.config.sync.lessonNotificationMinutes = value
            if (pl.szczodrzynski.edziennik.App.profileId != 0) viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                AximoLessonNotifications.scheduleTodayAndTomorrow(requireContext(), pl.szczodrzynski.edziennik.App.profileId)
            }
        }
        addAction(c, "Uprawnienia powiadomień", "Sprawdź lub nadaj dostęp Androidowi.", null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 47002)
            } else Toast.makeText(activity, "Powiadomienia są już dostępne.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addSchoolSettings(c: LinearLayout) {
        addSwitch(c, "Tryb szkolny", "Automatycznie reaguj na godziny lekcji.", "school_mode", true)
        addSilenceSchedulePreview(c)
        addChoiceAction(c, "Wycisz przed pierwszą lekcją", "Wybierz, ile minut wcześniej Aximo ma wyciszyć telefon.", "silence_before", intArrayOf(0, 5, 10, 15, 20, 30), 10) { rescheduleSilence() }
        addChoiceAction(c, "Przywróć dźwięk po ostatniej lekcji", "Wybierz, ile minut po lekcjach Aximo ma przywrócić poprzedni tryb.", "silence_after", intArrayOf(0, 5, 10, 15, 20, 30), 10) { rescheduleSilence() }
        addAction(c, "Wycisz telefon teraz", "Wycisz ręcznie na wybrany czas, niezależnie od planu lekcji.", null) {
            val values = intArrayOf(15, 30, 60, 120, 240)
            AlertDialog.Builder(requireContext()).setTitle("Wycisz telefon na").setSingleChoiceItems(
                values.map { if (it < 60) "$it min" else "${it / 60} godz." }.toTypedArray(), -1
            ) { dialog, which ->
                AximoLessonSilence.muteNowFor(requireContext(), values[which])
                Toast.makeText(activity, "Telefon wyciszony.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }.show()
        }
        addAction(c, "Odcisz telefon teraz", "Natychmiast przywróć poprzedni tryb dźwięku.", null) {
            AximoLessonSilence.manualUnmute(requireContext())
            Toast.makeText(activity, "Przywrócono dźwięk.", Toast.LENGTH_SHORT).show()
        }
        addAction(c, "Zezwól na automatyczne wyciszanie", "Nadaj Aximo dostęp potrzebny do wyciszania telefonu podczas lekcji.", null) {
            if (AximoLessonSilence.hasNotificationPolicyAccess(requireContext()))
                Toast.makeText(activity, "Dostęp jest już przyznany.", Toast.LENGTH_SHORT).show()
            else AximoLessonSilence.openNotificationPolicyAccessSettings(activity)
        }
    }

    private fun addSilenceSchedulePreview(c: LinearLayout) {
        val app = requireContext().applicationContext as pl.szczodrzynski.edziennik.App
        val before = prefs.getInt("silence_before", 10).coerceIn(0, 30)
        val after = prefs.getInt("silence_after", 10).coerceIn(0, 30)
        val profileId = pl.szczodrzynski.edziennik.App.profileId

        val times = if (profileId != 0) {
            try {
                val today = pl.szczodrzynski.edziennik.utils.models.Date.getToday()
                val lessons = app.db.timetableDao().getAllForDateNow(profileId, today)
                    .filter {
                        it.type != pl.szczodrzynski.edziennik.data.db.entity.Lesson.TYPE_CANCELLED &&
                        it.type != pl.szczodrzynski.edziennik.data.db.entity.Lesson.TYPE_NO_LESSONS
                    }
                    .mapNotNull {
                        val start = it.displayStartTime ?: return@mapNotNull null
                        val end = it.displayEndTime ?: return@mapNotNull null
                        today.getAsCalendar(start).timeInMillis to today.getAsCalendar(end).timeInMillis
                    }
                if (lessons.isNotEmpty()) {
                    val start = lessons.minOf { it.first } - before * 60_000L
                    val end = lessons.maxOf { it.second } + after * 60_000L
                    val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    "Dzisiaj Aximo wyciszy telefon: ${fmt.format(java.util.Date(start))}–${fmt.format(java.util.Date(end))}"
                } else "Dzisiaj brak lekcji — wyciszenie nie jest planowane."
            } catch (_: Exception) {
                "Nie udało się odczytać dzisiejszego planu lekcji."
            }
        } else {
            "Zaloguj się, aby wyświetlić godziny wyciszenia z planu lekcji."
        }

        addAction(
            c,
            "Wyciszenie według planu",
            "${times}  •  ${before} min przed / ${after} min po",
            null
        ) {
            Toast.makeText(activity, times, Toast.LENGTH_LONG).show()
        }
    }

    private fun addAppearanceSettings(c: LinearLayout) {
        addAction(c, "Otwórz personalizację", "Motyw, tło, kolory, zaokrąglenia, karty i animacje.", NavTarget.APPEARANCE)
    }

    private fun addAdvancedSettings(c: LinearLayout) {
        addAction(c, "Profil i konto", "Szkoła, konto oraz synchronizacja.", NavTarget.PROFILE_MANAGER)
        addAction(c, "Pomoc i informacje", "Pomoc, zgłaszanie problemów i informacje o Aximo.", NavTarget.HELP)
    }

    private fun addSwitch(
        container: LinearLayout,
        title: String,
        summary: String,
        key: String,
        default: Boolean
    ) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(13), dp(10), dp(13))
            background = cardBackground()
            isClickable = true
        }
        val textBox = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        val titleView = TextView(requireContext()).apply {
            text = title
            textSize = 15f
            setTextColor(Color.WHITE)
        }
        val summaryView = TextView(requireContext()).apply {
            text = summary
            textSize = 12f
            setTextColor(Color.rgb(127, 138, 168))
            setPadding(0, dp(3), 0, 0)
        }
        textBox.addView(titleView)
        textBox.addView(summaryView)
        row.addView(textBox, LinearLayout.LayoutParams(0, -2, 1f))
        val sw = Switch(requireContext()).apply {
            isChecked = prefs.getBoolean(key, default)
            buttonTintListCompat()
        }
        row.addView(sw, LinearLayout.LayoutParams(-2, -2))
        val toggle: () -> Unit = {
            sw.isChecked = !sw.isChecked
        }
        row.setOnClickListener { toggle() }
        sw.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            prefs.edit().putBoolean(key, checked).apply()
            activity.b.aximoBottomNavigation.refreshSettings()
            (activity.supportFragmentManager.findFragmentById(R.id.fragment) as? pl.szczodrzynski.edziennik.ui.home.HomeFragment)?.applyAximoSettings()
            if (key == "diary_swipe_refresh") {
                activity.swipeRefreshLayout.isEnabled = checked && activity.supportFragmentManager.findFragmentById(R.id.fragment) !is pl.szczodrzynski.edziennik.ui.timetable.TimetableFragment
            }
            when (key) {
                "school_mode" -> {
                    val app = requireContext().applicationContext as pl.szczodrzynski.edziennik.App
                    app.config.sync.automaticSilenceEnabled = checked
                    if (pl.szczodrzynski.edziennik.App.profileId != 0) {
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            AximoLessonSilence.scheduleTodayAndTomorrow(requireContext(), pl.szczodrzynski.edziennik.App.profileId)
                        }
                    }
                }
                "notify_next_lesson" -> {
                    val app = requireContext().applicationContext as pl.szczodrzynski.edziennik.App
                    app.config.sync.lessonNotificationsEnabled = checked
                    if (pl.szczodrzynski.edziennik.App.profileId != 0) {
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            AximoLessonNotifications.scheduleTodayAndTomorrow(requireContext(), pl.szczodrzynski.edziennik.App.profileId)
                        }
                    }
                }
            }
        }
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.topMargin = dp(8)
        container.addView(row, lp)
    }

    private fun addChoiceAction(container: LinearLayout, title: String, summary: String, key: String, values: IntArray, default: Int, onChanged: (Int) -> Unit) {
        addAction(container, title, "${summary} Teraz: ${prefs.getInt(key, default)} min", null) {
            val current = values.indexOf(prefs.getInt(key, default)).takeIf { it >= 0 } ?: 0
            AlertDialog.Builder(requireContext()).setTitle(title).setSingleChoiceItems(values.map { "$it min" }.toTypedArray(), current) { dialog, which ->
                val value = values[which]
                prefs.edit().putInt(key, value).apply()
                onChanged(value)
                dialog.dismiss()
                Toast.makeText(activity, "$title: $value min", Toast.LENGTH_SHORT).show()
            }.show()
        }
    }

    private fun rescheduleSilence() {
        if (pl.szczodrzynski.edziennik.App.profileId != 0) viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            AximoLessonSilence.scheduleTodayAndTomorrow(requireContext(), pl.szczodrzynski.edziennik.App.profileId)
        }
    }
    private fun addAction(
        container: LinearLayout,
        title: String,
        summary: String,
        target: NavTarget?,
        action: (() -> Unit)? = null
    ) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = cardBackground()
            isClickable = true
            setOnClickListener {
                if (action != null) action()
                else if (target != null) activity.navigate(navTarget = target)
            }
        }
        row.addView(TextView(requireContext()).apply {
            text = title
            textSize = 15f
            setTextColor(Color.WHITE)
        })
        row.addView(TextView(requireContext()).apply {
            text = summary + "   ›"
            textSize = 12f
            setTextColor(Color.rgb(165, 108, 255))
            setPadding(0, dp(3), 0, 0)
        })
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.topMargin = dp(8)
        container.addView(row, lp)
    }

    private fun showCategory(categories: List<Category>, selected: Int) {
        categories.forEachIndexed { index, category ->
            category.container.visibility = if (index == selected) View.VISIBLE else View.GONE
            val animationsEnabled = prefs.getBoolean("animationsEnabled", true)
            if (index == selected) {
                if (animationsEnabled) {
                    category.container.alpha = 0f
                    category.container.animate().alpha(1f).setDuration(180).start()
                } else {
                    category.container.alpha = 1f
                    category.container.animate().cancel()
                }
            } else {
                category.container.alpha = 1f
                category.container.animate().cancel()
            }
        }
        b.categoryTitle.text = categories[selected].title
        b.categoryDescription.text = categories[selected].description
        for (i in 0 until b.settingsTabs.childCount) {
            b.settingsTabs.getChildAt(i).background = tabBackground(i == selected)
        }
    }

    private fun tabBackground(selected: Boolean) = GradientDrawable().apply {
        cornerRadius = dp(20).toFloat()
        setColor(if (selected) Color.rgb(105, 55, 190) else Color.rgb(24, 23, 43))
        setStroke(dp(1), if (selected) Color.rgb(190, 135, 255) else Color.rgb(61, 52, 87))
    }

    private fun cardBackground() = GradientDrawable().apply {
        cornerRadius = dp(20).toFloat()
        startColor = Color.rgb(31, 25, 55)
        centerColor = Color.rgb(24, 22, 45)
        endColor = Color.rgb(18, 18, 35)
        setStroke(dp(1), Color.rgb(72, 52, 105))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun Switch.buttonTintListCompat() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.rgb(210, 205, 225))
            trackTintList = android.content.res.ColorStateList.valueOf(Color.rgb(80, 65, 110))
        }
    }
}
