package pl.szczodrzynski.edziennik.ui.aximo

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

object AximoAppearanceApplier {
    fun apply(root: View, context: android.content.Context) {
        val prefs = context.getSharedPreferences("aximo_appearance", 0)
        val style = AximoAppearanceStyle.fromOrdinal(prefs.getInt("style", AximoAppearanceStyle.AXIMO.ordinal))
        val accent = prefs.getInt("accentColor", style.accent)
        applyView(root, style, accent, true)
    }

    private fun applyView(view: View, style: AximoAppearanceStyle, accent: Int, isRoot: Boolean) {
        val bg = (view.background as? ColorDrawable)?.color
        if (isRoot || bg in ROOT_BACKGROUNDS) view.setBackgroundColor(style.background)
        else if (bg in SURFACE_BACKGROUNDS) view.setBackgroundColor(style.surface)

        if (view is TextView) {
            val color = view.currentTextColor
            when {
                color in PRIMARY_TEXTS -> view.setTextColor(style.text)
                color in MUTED_TEXTS -> view.setTextColor(blend(style.text, style.background, .55f))
                color in ACCENT_TEXTS -> view.setTextColor(accent)
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) applyView(view.getChildAt(i), style, accent, false)
        }
    }

    private fun blend(foreground: Int, background: Int, amount: Float): Int =
        Color.rgb(
            (Color.red(foreground) * amount + Color.red(background) * (1f - amount)).toInt(),
            (Color.green(foreground) * amount + Color.green(background) * (1f - amount)).toInt(),
            (Color.blue(foreground) * amount + Color.blue(background) * (1f - amount)).toInt()
        )

    private val ROOT_BACKGROUNDS = setOf(0xFF02091A.toInt(), 0xFF05081A.toInt(), 0xFF081127.toInt(), 0xFF11101A.toInt())
    private val SURFACE_BACKGROUNDS = setOf(0xFF121B33.toInt(), 0xFF0F172B.toInt(), 0xFF0F1830.toInt(), 0xFF201A31.toInt())
    private val PRIMARY_TEXTS = setOf(0xFFF7F5FF.toInt(), 0xFFF7F1FF.toInt(), 0xFFF4F1FF.toInt(), 0xFFF4F0FF.toInt(), 0xFFEDE9FF.toInt(), 0xFFF2EEF8.toInt())
    private val MUTED_TEXTS = setOf(0xFF8F9DBB.toInt(), 0xFF9AA7C4.toInt(), 0xFF7F8BA8.toInt(), 0xFFAEB8D4.toInt(), 0xFF9EA4BC.toInt(), 0xFF71809F.toInt())
    private val ACCENT_TEXTS = setOf(0xFFB58CFF.toInt(), 0xFFD9C6FF.toInt(), 0xFFD4BEFF.toInt(), 0xFFC9AEFF.toInt(), 0xFF8D72FF.toInt())
}