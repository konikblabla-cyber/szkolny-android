package pl.szczodrzynski.edziennik.ui.aximo

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import pl.szczodrzynski.edziennik.R

/**
 * Applies the Aximo appearance to already inflated views.
 *
 * The old implementation decided whether a card was an Aximo surface from the
 * VIEW id name (startsWith("aximo_")). Most Aximo layouts use normal ids such as
 * themeDark, settingsGrades, etc., so their backgrounds were never updated.
 * Appearance changes therefore appeared to do nothing except wallpaper.
 */
object AximoAppearanceApplier {
    fun apply(root: View, context: android.content.Context) {
        val prefs = context.getSharedPreferences("aximo_appearance", 0)
        val style = AximoAppearanceStyle.fromOrdinal(
            prefs.getInt("style", AximoAppearanceStyle.AXIMO.ordinal)
        )
        val accent = prefs.getInt("accentColor", style.accent)
        val roundness = prefs.getInt("cardRoundness", 18).coerceIn(4, 28)
        val transparency = prefs.getInt("surfaceTransparency", 18).coerceIn(0, 70)
        val softCards = prefs.getBoolean("softCards", false)
        val animationsEnabled = prefs.getBoolean("animationsEnabled", true)
        val animationStyle = prefs.getInt("animationStyle", 2).coerceIn(0, 3)
        val light = prefs.getString("theme", "dark") == "light"

        val effectiveBackground = if (light) 0xFFF7F7FB.toInt() else style.background
        val effectiveSurface = if (light) 0xFFFFFFFF.toInt() else style.surface
        val effectiveSurfaceAlt = if (light) 0xFFF0EEF7.toInt() else style.surfaceAlt
        val effectiveText = if (light) 0xFF202027.toInt() else style.text
        val effectiveMuted = if (light) 0xFF626276.toInt()
            else blend(style.text, style.background, .55f)

        applyView(
            root = root,
            style = style,
            accent = accent,
            isRoot = true,
            roundness = roundness,
            transparency = transparency,
            softCards = softCards,
            animationsEnabled = animationsEnabled,
            animationStyle = animationStyle,
            background = effectiveBackground,
            surface = effectiveSurface,
            surfaceAlt = effectiveSurfaceAlt,
            text = effectiveText,
            muted = effectiveMuted
        )
    }

    private fun applyView(
        view: View,
        style: AximoAppearanceStyle,
        accent: Int,
        isRoot: Boolean,
        roundness: Int,
        transparency: Int,
        softCards: Boolean,
        animationsEnabled: Boolean,
        animationStyle: Int,
        background: Int,
        surface: Int,
        surfaceAlt: Int,
        text: Int,
        muted: Int
    ) {
        if (view.id == R.id.styleGrid) return

        val original = view.background?.mutate()
        val entryName = runCatching {
            view.resources.getResourceEntryName(view.id)
        }.getOrNull().orEmpty()

        val isWallpaper =
            original is AximoPhotoWallpaperDrawable ||
                original is AximoAnimatedWallpaperDrawable

        // Aximo cards/buttons are mostly GradientDrawables. Do not rely on the
        // view id naming convention: many valid Aximo controls have generic ids.
        val isExcluded = entryName.startsWith("plan_lesson") ||
            entryName.startsWith("notification") ||
            entryName.contains("grade") && entryName.contains("background")

        val alpha = if (softCards) {
            (255 - transparency * 2).coerceIn(75, 255)
        } else {
            (255 - transparency).coerceIn(90, 255)
        }

        when {
            original is GradientDrawable && !isExcluded -> {
                original.cornerRadius =
                    roundness * view.resources.displayMetrics.density
                original.alpha = alpha
                original.setColor(
                    when {
                        entryName.contains("icon_button") -> surfaceAlt
                        entryName.contains("chip") -> surfaceAlt
                        else -> surface
                    }
                )
                view.background = original
            }

            original is ColorDrawable && !isExcluded -> {
                val bg = original.color
                if (bg in SURFACE_BACKGROUNDS || bg in ROOT_BACKGROUNDS ||
                    isLikelyAximoSurface(view)
                ) {
                    view.background = ColorDrawable(
                        Color.argb(alpha, Color.red(surface), Color.green(surface), Color.blue(surface))
                    )
                }
            }
        }

        if (isRoot && !isWallpaper) {
            // setAppBackground() normally supplies the wallpaper. If there is no
            // wallpaper, the selected style still controls the whole canvas.
            if (view.background !is AximoPhotoWallpaperDrawable &&
                view.background !is AximoAnimatedWallpaperDrawable
            ) {
                view.setBackgroundColor(background)
            }
        }

        if (view is TextView) {
            val color = view.currentTextColor
            when {
                color in ACCENT_TEXTS -> view.setTextColor(accent)
                color in MUTED_TEXTS -> view.setTextColor(muted)
                color in PRIMARY_TEXTS -> view.setTextColor(text)
            }
        }

        if (!isRoot && animationsEnabled && animationStyle > 0 &&
            view.isShown && view.alpha > 0f
        ) {
            val duration = when (animationStyle) {
                1 -> 110L
                2 -> 170L
                else -> 240L
            }
            val targetAlpha = view.alpha
            view.animate().cancel()
            view.alpha = 0.94f
            view.animate()
                .alpha(targetAlpha)
                .setDuration(duration)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyView(
                    view.getChildAt(i), style, accent, false, roundness,
                    transparency, softCards, animationsEnabled, animationStyle,
                    background, surface, surfaceAlt, text, muted
                )
            }
        }
    }

    private fun isLikelyAximoSurface(view: View): Boolean {
        val name = runCatching {
            view.resources.getResourceEntryName(view.id)
        }.getOrNull().orEmpty()
        return name.startsWith("settings") ||
            name.startsWith("theme") ||
            name.startsWith("bg") ||
            name.startsWith("custom") ||
            name.startsWith("animation") ||
            name.startsWith("wallpaper") ||
            name.startsWith("accent") ||
            name.startsWith("category")
    }

    private fun blend(foreground: Int, background: Int, amount: Float): Int =
        Color.rgb(
            (Color.red(foreground) * amount + Color.red(background) * (1f - amount)).toInt(),
            (Color.green(foreground) * amount + Color.green(background) * (1f - amount)).toInt(),
            (Color.blue(foreground) * amount + Color.blue(background) * (1f - amount)).toInt()
        )

    private val ROOT_BACKGROUNDS = setOf(
        0xFF02091A.toInt(), 0xFF05081A.toInt(),
        0xFF081127.toInt(), 0xFF11101A.toInt()
    )

    private val SURFACE_BACKGROUNDS = setOf(
        0xFF121B33.toInt(), 0xFF0F172B.toInt(),
        0xFF0F1830.toInt(), 0xFF201A31.toInt()
    )

    private val PRIMARY_TEXTS = setOf(
        0xFFF7F5FF.toInt(), 0xFFF7F1FF.toInt(), 0xFFF4F1FF.toInt(),
        0xFFF4F0FF.toInt(), 0xFFEDE9FF.toInt(), 0xFFF2EEF8.toInt(),
        0xFFFFFFFF.toInt()
    )

    private val MUTED_TEXTS = setOf(
        0xFF8F9DBB.toInt(), 0xFF9AA7C4.toInt(), 0xFF7F8BA8.toInt(),
        0xFFAEB8D4.toInt(), 0xFF9EA4BC.toInt(), 0xFF71809F.toInt(),
        0xFF69738F.toInt()
    )

    private val ACCENT_TEXTS = setOf(
        0xFFB58CFF.toInt(), 0xFFD9C6FF.toInt(), 0xFFD4BEFF.toInt(),
        0xFFC9AEFF.toInt(), 0xFF8D72FF.toInt()
    )
}
