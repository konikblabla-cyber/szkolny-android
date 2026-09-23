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
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.FragmentAximoSettingsBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.core.aximo.AximoLessonSilence
import pl.szczodrzynski.edziennik.core.aximo.AximoLessonNotifications
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import kotlinx.coroutines.Dispatchers
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
            Category("Ekran główny", "Co ma być widoczne po uruchomieniu Aximo.", b.categoryHome),
            Category("Nawigacja", "Sposób poruszania się po najważniejszych częściach dziennika.", b.categoryNavigation),
            Category("Dziennik", "Zachowanie planu, odświeżania i danych dziennika.", b.categoryDiary),
            Category("Oceny i frekwencja", "Jak Aximo pokazuje wyniki, średnie i obecności.", b.categoryGrades),
            Category("Wiadomości i zadania", "Szybki dostęp do wiadomości, prac domowych i informacji.", b.categoryMessages),
            Category("Powiadomienia", "Wybierz dokładnie, o czym Aximo ma Ci przypominać.", b.categoryNotifications),
            Category("Tryb szkolny", "Automatyczne zachowanie telefonu podczas lekcji.", b.categorySchool),
            Category("Personalizacja", "Dodatkowe możliwości dopasowania Aximo do siebie.", b.categoryPersonal),
            Category("Zaawansowane", "Opcje techniczne i zachowanie aplikacji.", b.categoryAdvanced)
        )

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
        addGradeSettings(b.categoryGrades)
        addMessageSettings(b.categoryMessages)
        addNotificationSettings(b.categoryNotifications)
        addSchoolSettings(b.categorySchool)
        addPersonalSettings(b.categoryPersonal)
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
        addSwitch(c, "Szybkie akcje", "Pokazuj skróty do najczęściej używanych ekranów.", "home_quick_actions", true)
        addAction(c, "Ustaw wygląd ekranu głównego", "Motywy, tło, kolory i układ kart.", NavTarget.APPEARANCE)
    }

    private fun addNavigationSettings(c: LinearLayout) {
        addSwitch(c, "Dolna nawigacja", "Pokazuj pasek na dole ekranu.", "nav_bottom", true)
        addSwitch(c, "Menu radialne", "Przytrzymaj dolny przycisk, aby otworzyć okrągłe menu.", "nav_radial", true)
        addSwitch(c, "Plan w nawigacji", "Dodaj Plan lekcji do szybkiego dostępu.", "nav_timetable", true)
        addSwitch(c, "Oceny w nawigacji", "Dodaj Oceny do szybkiego dostępu.", "nav_grades", true)
        addSwitch(c, "Frekwencja w nawigacji", "Dodaj Frekwencję do szybkiego dostępu.", "nav_attendance", true)
        addSwitch(c, "Wiadomości w nawigacji", "Dodaj Wiadomości do szybkiego dostępu.", "nav_messages", true)
        addSwitch(c, "Zadania w nawigacji", "Dodaj Zadania domowe do szybkiego dostępu.", "nav_homework", true)
        addSwitch(c, "Ustawienia w nawigacji", "Szybko otwieraj ten panel.", "nav_settings", true)
        addAction(c, "Edytuj układ skrótów", "Otwórz dodatkowe ustawienia szybkiego dostępu.", NavTarget.MORE)
    }

    private fun addDiarySettings(c: LinearLayout) {
        addSwitch(c, "Odświeżanie gestem", "Przeciągnięcie w dół odświeża dane.", "diary_swipe_refresh", true)
        addSwitch(c, "Automatyczne przewijanie do dziś", "Plan lekcji otwieraj od bieżącego dnia.", "diary_scroll_today", true)
    }

    private fun addGradeSettings(c: LinearLayout) {
    }

    private fun addMessageSettings(c: LinearLayout) {
        addSwitch(c, "Zadania na ekranie głównym", "Pokazuj najbliższe zadania bez otwierania sekcji.", "homework_on_home", true)
    }

    private fun addNotificationSettings(c: LinearLayout) {
        addSwitch(c, "Powiadomienie o następnej lekcji", "Pokazuj aktualną i następną lekcję.", "notify_next_lesson", true)
        addAction(c, "Uprawnienia powiadomień", "Sprawdź lub nadaj dostęp Androidowi.", null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 47002)
            } else Toast.makeText(activity, "Powiadomienia są już dostępne.", Toast.LENGTH_SHORT).show()
        }
        addAction(c, "Precyzyjne przypomnienia", "Dostęp Androida potrzebny do punktualnych alarmów.", null) {
            if (AximoLessonNotifications.canScheduleExactAlarms(requireContext()))
                Toast.makeText(activity, "Dostęp jest już przyznany.", Toast.LENGTH_SHORT).show()
            else AximoLessonNotifications.openExactAlarmSettings(activity)
        }
    }

    private fun addSchoolSettings(c: LinearLayout) {
        addSwitch(c, "Tryb szkolny", "Automatycznie reaguj na godziny lekcji.", "school_mode", true)
        addAction(c, "Dostęp systemowy trybu szkolnego", "Opcjonalny dostęp do specjalnych trybów Androida.", null) {
            if (AximoLessonSilence.hasNotificationPolicyAccess(requireContext()))
                Toast.makeText(activity, "Dostęp jest już przyznany.", Toast.LENGTH_SHORT).show()
            else AximoLessonSilence.openNotificationPolicyAccessSettings(activity)
        }
    }

    private fun addPersonalSettings(c: LinearLayout) {
        addAction(c, "Pełna personalizacja wyglądu", "Motywy, kolory, 5 teł, przezroczystość i styl kart.", NavTarget.APPEARANCE)
        addAction(c, "Profil i konto", "Szkoła, konto oraz synchronizacja.", NavTarget.PROFILE_MANAGER)
    }

    private fun addAdvancedSettings(c: LinearLayout) {
        addAction(c, "Układ i szybki dostęp", "Dodatkowe ustawienia skrótów i sekcji.", NavTarget.MORE)
        addAction(c, "Pomoc i wsparcie", "Instrukcja i zgłaszanie problemów.", NavTarget.HELP)
        addAction(c, "O Aximo", "Wersja aplikacji i informacje.", NavTarget.ABOUT)
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
            category.container.alpha = if (index == selected) 0f else 1f
            if (index == selected) category.container.animate().alpha(1f).setDuration(180).start()
        }
        b.categoryTitle.text = categories[selected].title
        b.categoryDescription.text = categories[selected].description
        for (i in 0 until b.settingsTabs.childCount) {
            b.settingsTabs.getChildAt(i).background = tabBackground(i == selected)
        }
    }

    private fun tabBackground(selected: Boolean) = GradientDrawable().apply {
        cornerRadius = dp(20).toFloat()
        setColor(if (selected) Color.rgb(105, 55, 190) else Color.rgb(18, 23, 47))
        setStroke(dp(1), if (selected) Color.rgb(165, 108, 255) else Color.rgb(43, 51, 82))
    }

    private fun cardBackground() = GradientDrawable().apply {
        cornerRadius = dp(18).toFloat()
        setColor(Color.rgb(12, 17, 38))
        setStroke(dp(1), Color.rgb(31, 39, 67))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun Switch.buttonTintListCompat() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.rgb(210, 205, 225))
            trackTintList = android.content.res.ColorStateList.valueOf(Color.rgb(80, 65, 110))
        }
    }
}
