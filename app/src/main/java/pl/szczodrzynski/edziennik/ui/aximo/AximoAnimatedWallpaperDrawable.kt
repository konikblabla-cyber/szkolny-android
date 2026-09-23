package pl.szczodrzynski.edziennik.ui.aximo

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.sin

/**
 * Animated Aximo wallpaper: slow moving glows, particles and waves.
 * It is intentionally lightweight so it can run continuously on a phone.
 */
class AximoAnimatedWallpaperDrawable(
    private val preset: String,
    private val colors: IntArray
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 14000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidateSelf()
        }
    }

    init {
        animator.start()
    }

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat().coerceAtLeast(1f)
        val h = bounds.height().toFloat().coerceAtLeast(1f)
        val p = phase * 6.2831855f

        paint.shader = LinearGradient(
            0f, 0f, w, h,
            colors,
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, paint)

        val glowColors = when (preset) {
            "forest" -> intArrayOf(0x8857D59A.toInt(), 0x003C8B5C)
            "sea", "rain" -> intArrayOf(0x8842C9D8.toInt(), 0x003D6FA8)
            "sunset", "ember" -> intArrayOf(0x88FF744A.toInt(), 0x003B1A5E)
            "lavender", "cosmos", "stars" -> intArrayOf(0x888D72FF.toInt(), 0x003B62D8)
            "neon" -> intArrayOf(0x885BFFDC.toInt(), 0x004A7CFF)
            "mist" -> intArrayOf(0x888FA6C9.toInt(), 0x003C4D75)
            "mountains" -> intArrayOf(0x888D72FF.toInt(), 0x002F245B)
            else -> intArrayOf(0x888D72FF.toInt(), 0x0042C9D8)
        }

        fun glow(x: Float, y: Float, radius: Float, alphaScale: Float = 1f) {
            paint.shader = RadialGradient(
                x, y, radius,
                intArrayOf(
                    ColorUtilsCompat.multiplyAlpha(glowColors[0], alphaScale),
                    glowColors[1]
                ),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(x, y, radius, paint)
        }

        val x1 = w * (0.18f + 0.14f * sin(p))
        val y1 = h * (0.22f + 0.10f * sin(p * 0.7f))
        val x2 = w * (0.78f + 0.12f * sin(p * 0.8f + 2f))
        val y2 = h * (0.62f + 0.12f * sin(p * 0.55f + 1f))
        glow(x1, y1, w * 0.42f, 0.72f)
        glow(x2, y2, w * 0.38f, 0.58f)

        if (preset == "mountains") {
            paint.shader = null
            paint.color = 0x663B285E
            val shift = sin(p) * w * 0.025f
            val path = android.graphics.Path().apply {
                moveTo(-w * .1f + shift, h)
                lineTo(w * .12f + shift, h * .57f)
                lineTo(w * .28f + shift, h * .76f)
                lineTo(w * .48f + shift, h * .43f)
                lineTo(w * .72f + shift, h * .73f)
                lineTo(w * 1.1f + shift, h * .5f)
                lineTo(w * 1.1f, h)
                close()
            }
            canvas.drawPath(path, paint)
        }

        if (preset == "stars" || preset == "cosmos" || preset == "neon") {
            paint.shader = null
            for (i in 0 until 28) {
                val x = ((i * 83) % 100) / 100f * w
                val y = ((i * 47) % 100) / 100f * h
                val twinkle = 0.45f + 0.55f * ((sin(p * (1f + i % 3 * .2f) + i) + 1f) / 2f)
                paint.color = Color.argb((110 * twinkle).toInt(), 220, 225, 255)
                canvas.drawCircle(x, y, 1.2f + (i % 3) * .6f, paint)
            }
        }

        paint.shader = null
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) { paint.colorFilter = colorFilter }
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)
        invalidateSelf()
    }

    fun stop() {
        animator.cancel()
    }

    private object ColorUtilsCompat {
        fun multiplyAlpha(color: Int, factor: Float): Int {
            val a = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
            return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
        }
    }
}
