package pl.szczodrzynski.edziennik.ui.aximo

import android.os.Bundle
import android.view.View
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.enums.Theme
import pl.szczodrzynski.edziennik.databinding.FragmentAximoAppearanceBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment

class AximoAppearanceFragment : BaseFragment<FragmentAximoAppearanceBinding, MainActivity>(
    inflater = FragmentAximoAppearanceBinding::inflate,
) {
    private val prefs by lazy {
        requireContext().getSharedPreferences("aximo_appearance", 0)
    }

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        b.backButton.setOnClickListener {
            activity.onBackPressedDispatcher.onBackPressed()
        }

        val theme = prefs.getString("theme", "dark") ?: "dark"
        val accent = prefs.getString("accent", "purple") ?: "purple"
        val background = prefs.getString("background", "default") ?: "default"

        setTheme(theme)
        setAccent(accent)
        setBackground(background)

        b.themeDark.setOnClickListener { saveTheme("dark") }
        b.themeLight.setOnClickListener { saveTheme("light") }
        b.themeAuto.setOnClickListener { saveTheme("auto") }

        val accents = mapOf(
            b.accentPurple to "purple", b.accentBlue to "blue", b.accentCyan to "cyan",
            b.accentGreen to "green", b.accentYellow to "yellow", b.accentOrange to "orange",
            b.accentPink to "pink"
        )
        accents.forEach { (view, value) ->
            view.setOnClickListener { saveAccent(value) }
        }

        val backgrounds = mapOf(
            b.bgDefault to "default", b.bgMountains to "mountains",
            b.bgSea to "sea", b.bgCity to "city"
        )
        backgrounds.forEach { (view, value) ->
            view.setOnClickListener { saveBackground(value) }
        }
    }

    private fun saveTheme(value: String) {
        prefs.edit().putString("theme", value).apply()
        app.config.ui.themeNightMode = when (value) {
            "light" -> false
            "dark" -> true
            else -> null
        }
        setTheme(value)
        requireActivity().recreate()
    }

    private fun saveAccent(value: String) {
        prefs.edit().putString("accent", value).apply()
        app.config.ui.themeColor = when (value) {
            "blue" -> Theme.BLUE
            "green" -> Theme.GREEN
            "cyan" -> Theme.TEAL
            "orange", "pink" -> Theme.RED
            else -> Theme.PURPLE
        }
        setAccent(value)
        requireActivity().recreate()
    }

    private fun saveBackground(value: String) {
        prefs.edit().putString("background", value).apply()
        setBackground(value)
    }

    private fun setTheme(value: String) {
        val selected = when (value) {
            "light" -> b.themeLight
            "auto" -> b.themeAuto
            else -> b.themeDark
        }
        listOf(b.themeDark, b.themeLight, b.themeAuto).forEach {
            it.alpha = if (it == selected) 1f else 0.55f
        }
        b.appearanceSaved.text = "Motyw: " + when (value) {
            "light" -> "Jasny"
            "auto" -> "Automatyczny"
            else -> "Ciemny"
        } + " · zapisano"
    }

    private fun setAccent(value: String) {
        val colors = mapOf(
            "purple" to 0xFF8D72FF.toInt(), "blue" to 0xFF4D8DFF.toInt(),
            "cyan" to 0xFF34D5E8.toInt(), "green" to 0xFF58D68D.toInt(),
            "yellow" to 0xFFF2C94C.toInt(), "orange" to 0xFFF2994A.toInt(),
            "pink" to 0xFFE56BFF.toInt()
        )
        val views = listOf(b.accentPurple,b.accentBlue,b.accentCyan,b.accentGreen,b.accentYellow,b.accentOrange,b.accentPink)
        val names = listOf("purple","blue","cyan","green","yellow","orange","pink")
        views.forEachIndexed { i, v -> v.scaleX = if (names[i] == value) 1.18f else 1f; v.scaleY = if (names[i] == value) 1.18f else 1f }
        b.appearanceSaved.text = "Kolor: " + value + " · zapisano"
    }

    private fun setBackground(value: String) {
        val views: List<View> = listOf(b.bgDefault,b.bgMountains,b.bgSea,b.bgCity)
        val names = listOf("default","mountains","sea","city")
        views.forEachIndexed { i, v -> v.alpha = if (names[i] == value) 1f else 0.6f }
    }
}