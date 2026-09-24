package pl.szczodrzynski.edziennik.ui.aximo

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import pl.szczodrzynski.edziennik.R

object AximoAppearanceApplier {
    fun apply(root: View, context: android.content.Context) {
        val prefs = context.getSharedPreferences("aximo_appearance", 0)
        val style = AximoAppearanceStyle.fromOrdinal(prefs.getInt("style", AximoAppearanceStyle.AXIMO.ordinal))
        val accent = prefs.getInt("accentColor", style.accent)
        val roundness = prefs.getInt("cardRoundness", 18).coerceIn(4, 28)
        val softCards = prefs.getBoolean("softCards", false)
        applyView(root, style, accent, true, roundness, softCards)
    }

    private fun applyView(view: View, style: AximoAppearanceStyle, accent: Int, isRoot: Boolean, roundness: Int, softCards: Boolean) {
        // The appearance picker contains its own 20 preview cards; never flatten them into the selected style.
        if (view.id == R.id.styleGrid) return
        val drawable = view.background?.mutate()
        val entryName = runCatching { view.resources.getResourceEntryName(view.id) }.getOrNull().orEmpty()
        if (drawable is GradientDrawable && view.id != R.id.styleGrid) {
            drawable.cornerRadius = roundness * view.resources.displayMetrics.density
            if (softCards) drawable.alpha = 205 else drawable.alpha = 255

            // Apply the selected preset to Aximo surfaces, while deliberately
            // preserving lesson/notification/subject-specific colored cards.
            val isAximoSurface = entryName.startsWith("aximo_") &&
                !entryName.startsWith("aximo_plan_lesson_bg_") &&
                !entryName.startsWith("aximo_notification_bg_") &&
                !entryName.contains("subject")
            if (isAximoSurface) {
                drawable.setColor(style.surface)
            }
            view.background = drawable
        }
        val bg = (drawable as? ColorDrawable)?.color
        when {
            bg in ROOT_BACKGROUNDS -> view.setBackgroundColor(style.background)
            bg in SURFACE_BACKGROUNDS -> view.setBackgroundColor(style.surface)
        }
        if (isRoot) {
            // Root is always the selected style's base, not a hard-coded purple.
            view.setBackgroundColor(style.background)
        }

        if (view is TextView) {
            val color = view.currentTextColor
            when {
                color in PRIMARY_TEXTS -> view.setTextColor(style.text)
                color in MUTED_TEXTS -> view.setTextColor(blend(style.text, style.background, .55f))
                color in ACCENT_TEXTS -> view.setTextColor(accent)
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) applyView(view.getChildAt(i), style, accent, false, roundness, softCards)
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