package pl.szczodrzynski.edziennik.ui.aximo

import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.enums.Theme
import pl.szczodrzynski.edziennik.databinding.FragmentAximoAppearanceBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import java.io.File
import java.io.FileOutputStream

class AximoAppearanceFragment : BaseFragment<FragmentAximoAppearanceBinding, MainActivity>(
    inflater = FragmentAximoAppearanceBinding::inflate,
) {
    private val prefs by lazy {
        requireContext().getSharedPreferences("aximo_appearance", 0)
    }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        try {
            val file = File(requireContext().filesDir, "aximo_custom_background.jpg")
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            prefs.edit().putString("background", "custom").apply()
            app.config.ui.appBackground = file.absolutePath
            b.appearanceSaved.text = "Własne zdjęcie zapisane ✓"
            requireActivity().recreate()
        } catch (_: Exception) {
            b.appearanceSaved.text = "Nie udało się zapisać zdjęcia"
        }
    }

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        b.backButton.setOnClickListener {
            activity.onBackPressedDispatcher.onBackPressed()
        }

        val theme = prefs.getString("theme", "dark") ?: "dark"
        val accent = prefs.getString("accent", "purple") ?: "purple"
        val background = prefs.getString("background", "default") ?: "default"
        val style = prefs.getInt("style", AximoAppearanceStyle.AXIMO.ordinal)

        setTheme(theme)
        setAccent(accent)
        setBackground(background)
        buildStyleGrid(style)
        applyCurrentAppearance()
        updatePreview(style)

        b.themeDark.setOnClickListener { saveTheme("dark") }
        b.themeLight.setOnClickListener { saveTheme("light") }
        b.themeAuto.setOnClickListener { saveTheme("auto") }

        mapOf(
            b.accentPurple to "purple", b.accentBlue to "blue", b.accentCyan to "cyan",
            b.accentGreen to "green", b.accentYellow to "yellow", b.accentOrange to "orange",
            b.accentPink to "pink"
        ).forEach { (view, value) -> view.setOnClickListener { saveAccent(value) } }

        mapOf(
            b.bgDefault to "default", b.bgMountains to "mountains",
            b.bgSea to "sea", b.bgCity to "city", b.bgAbstract to "abstract"
        ).forEach { (view, value) -> view.setOnClickListener { saveBackground(value) } }

        b.bgCustom.setOnClickListener {
            imagePicker.launch("image/*")
        }
    }

    private fun buildStyleGrid(selected: Int) {
        b.styleGrid.removeAllViews()
        AximoAppearanceStyle.entries.forEachIndexed { index, style ->
            val card = TextView(requireContext()).apply {
                text = "  " + style.title + "\n  " + if (index == selected) "✓ Wybrany" else "Dotknij, aby wybrać"
                setTextColor(style.text)
                textSize = 13f
                setPadding(10, 10, 10, 10)
                gravity = android.view.Gravity.CENTER_VERTICAL
                background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(style.surfaceAlt, style.surface)
                ).apply {
                    cornerRadius = 18f
                    setStroke(if (index == selected) 3 else 1, if (index == selected) style.accent else style.accentSoft)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener { saveStyle(index) }
            }
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = 78
                columnSpec = GridLayout.spec(index % 2, 1f)
                rowSpec = GridLayout.spec(index / 2)
                setMargins(0, 0, 6, 7)
            }
            b.styleGrid.addView(card, lp)
        }
    }

    private fun applyCurrentAppearance() {
        val style = AximoAppearanceStyle.fromOrdinal(prefs.getInt("style", AximoAppearanceStyle.AXIMO.ordinal))
        val accent = prefs.getInt("accentColor", style.accent)
        activity.setAppBackground()
        b.root.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        b.styleGrid.setBackgroundColor(style.background)
        b.accentRow.background = GradientDrawable().apply { setColor(style.surface); cornerRadius = 20f }
        b.appearanceSaved.setTextColor(accent)
    }

    private fun updatePreview(index: Int) {
        val s = AximoAppearanceStyle.fromOrdinal(index)
        b.styleGrid.setBackgroundColor(s.background)
        b.appearanceSaved.setTextColor(s.accent)
    }

    private fun saveStyle(index: Int) {
        prefs.edit().putInt("style", index).apply()
        prefs.edit().putInt("accentColor", AximoAppearanceStyle.fromOrdinal(index).accent).apply()
        val style = AximoAppearanceStyle.fromOrdinal(index)
        b.appearanceSaved.text = "Styl: " + style.title + " · zapisano ✓"
        updatePreview(index)
        applyCurrentAppearance()
        requireActivity().recreate()
    }

    private fun saveTheme(value: String) {
        prefs.edit().putString("theme", value).apply()
        applyCurrentAppearance()
        app.config.ui.themeNightMode = when (value) {
            "light" -> false
            "dark" -> true
            else -> null
        }
        setTheme(value)
        requireActivity().recreate()
    }

    private fun saveAccent(value: String) {
        val accentColor = accentColorFor(value)
        prefs.edit()
            .putString("accent", value)
            .putInt("accentColor", accentColor)
            .apply()
        applyCurrentAppearance()
        app.config.ui.themeColor = when (value) {
            "blue" -> Theme.BLUE
            "green" -> Theme.GREEN
            "cyan" -> Theme.TEAL
            "orange" -> Theme.RED
            "pink" -> Theme.RED
            else -> Theme.PURPLE
        }
        setAccent(value)
        requireActivity().recreate()
    }

    private fun accentColorFor(value: String): Int = when (value) {
        "blue" -> 0xFF5B8DFF.toInt()
        "cyan" -> 0xFF42C9D8.toInt()
        "green" -> 0xFF61C58A.toInt()
        "yellow" -> 0xFFD7B451.toInt()
        "orange" -> 0xFFD58A50.toInt()
        "pink" -> 0xFFD77ABF.toInt()
        else -> 0xFF8D72FF.toInt()
    }

    private fun saveBackground(value: String) {
        prefs.edit().putString("background", value).apply()
        if (value != "custom") app.config.ui.appBackground = null
        setBackground(value)
        activity.setAppBackground()
        applyCurrentAppearance()
        requireActivity().recreate()
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
    }

    private fun setAccent(value: String) {
        val views = listOf(b.accentPurple,b.accentBlue,b.accentCyan,b.accentGreen,b.accentYellow,b.accentOrange,b.accentPink)
        val names = listOf("purple","blue","cyan","green","yellow","orange","pink")
        views.forEachIndexed { i, v ->
            v.scaleX = if (names[i] == value) 1.18f else 1f
            v.scaleY = if (names[i] == value) 1.18f else 1f
            v.alpha = if (names[i] == value) 1f else 0.72f
        }
    }

    private fun setBackground(value: String) {
        val views: List<View> = listOf(b.bgDefault,b.bgMountains,b.bgSea,b.bgCity,b.bgAbstract,b.bgCustom)
        val names = listOf("default","mountains","sea","city","abstract","custom")
        views.forEachIndexed { i, v ->
            v.alpha = if (names[i] == value) 1f else 0.58f
        }
    }
}
