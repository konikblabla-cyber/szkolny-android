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
        addSwitch(c, "Pokazuj liczbę nieprzeczytanych", "Dodaj liczniki przy wiadomościach i zadaniach.", "home_unread_badges", true)
        addSwitch(c, "Automatycznie odświeżaj po wejściu", "Odśwież dane, gdy wracasz na start.", "home_refresh_on_resume", true)
        addSwitch(c, "Kompaktowy ekran główny", "Zmniejsz wysokość kart, aby zobaczyć więcej naraz.", "home_compact", false)
        addAction(c, "Ustaw wygląd ekranu głównego", "Motywy, tło, kolory i układ kart.", NavTarget.APPEARANCE)
    }

    private fun addNavigationSettings(c: LinearLayout) {
        addSwitch(c, "Dolna nawigacja", "Pokazuj pasek na dole ekranu.", "nav_bottom", true)
        addSwitch(c, "Menu radialne", "Przytrzymaj dolny przycisk, aby otworzyć okrągłe menu.", "nav_radial", true)
        addSwitch(c, "Animacje nawigacji", "Włącz płynne przejścia między ekranami.", "nav_animations", true)
        addSwitch(c, "Podświetlenie aktywnej sekcji", "Wyraźnie zaznacz aktualnie otwarty ekran.", "nav_active_highlight", true)
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
        addSwitch(c, "Odświeżaj automatycznie", "Pozwól Aximo odświeżać dane w tle.", "diary_auto_refresh", true)
        addSwitch(c, "Pokazuj salę przy lekcji", "Dodaj numer sali do planu.", "diary_show_room", true)
        addSwitch(c, "Pokazuj nauczyciela", "Dodaj nauczyciela do szczegółów lekcji.", "diary_show_teacher", true)
        addSwitch(c, "Pokazuj przedmiot na pasku", "Ułatwia szybkie rozpoznanie aktualnej lekcji.", "diary_subject_bar", true)
        addSwitch(c, "Zachowaj ostatni ekran", "Po uruchomieniu wróć do ostatnio używanej sekcji.", "diary_last_screen", false)
        addSwitch(c, "Potwierdzaj wyjście z edycji", "Pytaj przed porzuceniem niezapisanych zmian.", "diary_confirm_exit", true)
        addSwitch(c, "Pokazuj dni wolne", "Uwzględniaj dni wolne w planie.", "diary_free_days", true)
        addSwitch(c, "Pokazuj zastępstwa", "Wyróżniaj zastępstwa w planie lekcji.", "diary_substitutions", true)
        addSwitch(c, "Automatyczne przewijanie do dziś", "Plan lekcji otwieraj od bieżącego dnia.", "diary_scroll_today", true)
    }

    private fun addGradeSettings(c: LinearLayout) {
        addSwitch(c, "Pokazuj średnią", "Wyświetlaj średnią tam, gdzie jest dostępna.", "grades_average", true)
        addSwitch(c, "Koloruj oceny", "Używaj kolorów do szybkiego rozpoznania ocen.", "grades_colors", true)
        addSwitch(c, "Pokazuj wagi", "Wyświetlaj wagę oceny przy szczegółach.", "grades_weights", true)
        addSwitch(c, "Pokazuj komentarze", "Pokazuj komentarze nauczycieli pod ocenami.", "grades_comments", true)
        addSwitch(c, "Najnowsze oceny na górze", "Sortuj nowe oceny od najnowszych.", "grades_newest_first", true)
        addSwitch(c, "Pokazuj przewidywaną średnią", "Pokazuj wyliczenia planera ocen.", "grades_planned_average", true)
        addSwitch(c, "Pokazuj procent frekwencji", "Wyróżnij aktualny procent obecności.", "attendance_percentage", true)
        addSwitch(c, "Pokazuj nieobecności", "Pokazuj szczegółową liczbę nieobecności.", "attendance_absences", true)
        addSwitch(c, "Pokazuj spóźnienia", "Uwzględniaj spóźnienia w podsumowaniu.", "attendance_late", true)
        addSwitch(c, "Pokazuj usprawiedliwienia", "Rozdzielaj usprawiedliwione i nieusprawiedliwione.", "attendance_excused", true)
    }

    private fun addMessageSettings(c: LinearLayout) {
        addSwitch(c, "Znacznik nieprzeczytanych", "Pokazuj liczbę nieprzeczytanych wiadomości.", "messages_unread", true)
        addSwitch(c, "Najnowsze wiadomości na górze", "Sortuj rozmowy od najnowszej.", "messages_newest", true)
        addSwitch(c, "Podgląd treści wiadomości", "Pokaż krótki fragment na liście.", "messages_preview", true)
        addSwitch(c, "Podgląd nadawcy", "Zawsze pokazuj autora wiadomości.", "messages_sender", true)
        addSwitch(c, "Zadania na ekranie głównym", "Pokazuj najbliższe zadania bez otwierania sekcji.", "homework_on_home", true)
        addSwitch(c, "Sortuj zadania według terminu", "Najbliższy termin zawsze będzie pierwszy.", "homework_deadline_sort", true)
        addSwitch(c, "Pokazuj wykonane zadania", "Nie ukrywaj automatycznie zakończonych prac.", "homework_done", false)
        addSwitch(c, "Podświetlaj pilne zadania", "Wyróżniaj zadania z bliskim terminem.", "homework_urgent", true)
        addSwitch(c, "Pokazuj przedmiot zadania", "Dodaj nazwę przedmiotu na liście.", "homework_subject", true)
        addSwitch(c, "Powiadamiaj o nowych zadaniach", "Pozwól Aximo przypominać o nowych pracach.", "homework_notifications", true)
    }

    private fun addNotificationSettings(c: LinearLayout) {
        addSwitch(c, "Powiadomienie o następnej lekcji", "Pokazuj aktualną i następną lekcję.", "notify_next_lesson", true)
        addSwitch(c, "Powiadomienie o zadaniu", "Informuj o nowych lub zbliżających się zadaniach.", "notify_homework", true)
        addSwitch(c, "Powiadomienie o wiadomości", "Informuj o nowych wiadomościach.", "notify_messages", true)
        addSwitch(c, "Powiadomienie o zmianie planu", "Informuj o zmianach i zastępstwach.", "notify_schedule_changes", true)
        addSwitch(c, "Ciche powiadomienia", "Nie odtwarzaj dźwięku dla powiadomień Aximo.", "notify_silent", false)
        addSwitch(c, "Wibracje powiadomień", "Włącz wibrację dla ważnych alertów.", "notify_vibration", true)
        addSwitch(c, "Pokazuj szczegóły na ekranie blokady", "Pokazuj nazwę lekcji i podstawowe informacje.", "notify_lock_details", true)
        addSwitch(c, "Aktualizuj powiadomienie lekcji", "Jedno powiadomienie będzie aktualizowane zamiast tworzenia wielu.", "notify_update_existing", true)
        addSwitch(c, "Powiadamiaj o frekwencji", "Informuj o zmianach danych frekwencji.", "notify_attendance", false)
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
        addSwitch(c, "Wycisz przed lekcją", "Wycisz telefon 10 minut przed rozpoczęciem.", "school_mute_before", true)
        addSwitch(c, "Przywróć dźwięk po lekcjach", "Przywróć normalny dźwięk po zakończeniu całego dnia.", "school_restore_after", true)
        addSwitch(c, "Pokazuj status trybu szkolnego", "Pokaż, czy automatyzacja jest aktywna.", "school_status", true)
        addSwitch(c, "Pomijaj dni wolne", "Nie uruchamiaj automatyzacji w dni wolne.", "school_skip_free_days", true)
        addSwitch(c, "Uwzględniaj zastępstwa", "Dopasuj wyciszanie do aktualnego planu.", "school_substitutions", true)
        addSwitch(c, "Powiadom przed wyciszeniem", "Pokaż informację przed automatycznym wyciszeniem.", "school_mute_notice", true)
        addSwitch(c, "Powiadom po przywróceniu dźwięku", "Poinformuj po zakończeniu dnia szkolnego.", "school_restore_notice", false)
        addSwitch(c, "Testuj wyciszenie automatycznie", "Pozwól sprawdzać działanie automatyzacji.", "school_auto_test", false)
        addAction(c, "Dostęp systemowy trybu szkolnego", "Opcjonalny dostęp do specjalnych trybów Androida.", null) {
            if (AximoLessonSilence.hasNotificationPolicyAccess(requireContext()))
                Toast.makeText(activity, "Dostęp jest już przyznany.", Toast.LENGTH_SHORT).show()
            else AximoLessonSilence.openNotificationPolicyAccessSettings(activity)
        }
    }

    private fun addPersonalSettings(c: LinearLayout) {
        addSwitch(c, "Pokazuj zwierzaka Aximo", "Wyświetlaj wybranego peta w interfejsie.", "personal_pet", true)
        addSwitch(c, "Animowany zwierzak", "Pozwól petowi delikatnie się animować.", "personal_pet_animation", true)
        addSwitch(c, "Efekty przy kliknięciu", "Dodaj lekkie efekty dotyku do kart.", "personal_touch_effects", true)
        addSwitch(c, "Efekt powiadomień", "Dodaj wizualne wyróżnienie nowych informacji.", "personal_notification_fx", true)
        addSwitch(c, "Pamiętaj ostatni wybór", "Zapamiętuj ostatnio wybraną sekcję lub filtr.", "personal_remember_filters", true)
        addSwitch(c, "Duże elementy dotykowe", "Zwiększ obszary klikalne dla wygodniejszej obsługi.", "personal_large_touch", false)
        addSwitch(c, "Pokazuj wskazówki", "Wyświetlaj krótkie podpowiedzi przy nowych funkcjach.", "personal_tips", true)
        addSwitch(c, "Potwierdzaj ważne akcje", "Pytaj przed resetowaniem lub usuwaniem danych.", "personal_confirm_actions", true)
        addAction(c, "Pełna personalizacja wyglądu", "Motywy, kolory, 5 teł, przezroczystość i styl kart.", NavTarget.APPEARANCE)
        addAction(c, "Profil i konto", "Szkoła, konto oraz synchronizacja.", NavTarget.PROFILE_MANAGER)
    }

    private fun addAdvancedSettings(c: LinearLayout) {
        addSwitch(c, "Odświeżaj po powrocie do aplikacji", "Aktualizuj dane po przejściu z innej aplikacji.", "advanced_refresh_resume", true)
        addSwitch(c, "Zachowaj pozycję przewijania", "Wracając do sekcji, zostań w ostatnim miejscu.", "advanced_keep_scroll", true)
        addSwitch(c, "Szybsze animacje", "Skróć czas animacji interfejsu.", "advanced_fast_animations", false)
        addSwitch(c, "Oszczędzanie baterii", "Ogranicz zbędne odświeżanie w tle.", "advanced_battery", true)
        addSwitch(c, "Ogranicz dane w tle", "Zmniejsz częstotliwość synchronizacji poza Wi-Fi.", "advanced_mobile_data", false)
        addSwitch(c, "Automatyczne ponawianie synchronizacji", "Spróbuj ponownie po chwilowym błędzie.", "advanced_retry", true)
        addSwitch(c, "Pokazuj informacje diagnostyczne", "Dodatkowe informacje pomocne przy zgłaszaniu błędów.", "advanced_diagnostics", false)
        addSwitch(c, "Logowanie błędów", "Zapisuj lokalnie podstawowe informacje o błędach.", "advanced_error_log", false)
        addSwitch(c, "Nie usypiaj ekranu podczas planu", "Ekran pozostanie aktywny podczas przeglądania planu.", "advanced_keep_screen", false)
        addSwitch(c, "Automatycznie wybieraj bieżący tydzień", "Po otwarciu planu ustaw aktualny tydzień.", "advanced_current_week", true)
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
