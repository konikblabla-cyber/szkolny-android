package pl.szczodrzynski.edziennik.ui.aximo

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.math.sin

/**
 * Real-photo Aximo wallpaper with a very slow cinematic pan/zoom.
 * Photos are cached locally after the first download and a gradient fallback
 * is shown immediately, so the UI never waits for the network.
 */
class AximoPhotoWallpaperDrawable(
    private val context: Context,
    private val preset: String,
    private val fallbackColors: IntArray
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var bitmap: Bitmap? = null
    private var phase = 0f
    private var alphaValue = 255
    private val animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 18000L
        repeatCount = android.animation.ValueAnimator.INFINITE
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidateSelf()
        }
    }

    init {
        animator.start()
        loadPhoto()
    }

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat().coerceAtLeast(1f)
        val h = bounds.height().toFloat().coerceAtLeast(1f)
        val p = phase * Math.PI * 2.0
        val b = bitmap

        if (b == null) {
            paint.shader = LinearGradient(0f, 0f, w, h, fallbackColors, null, Shader.TileMode.CLAMP)
            paint.alpha = alphaValue
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
            return
        }

        val bw = b.width.toFloat()
        val bh = b.height.toFloat()
        val baseScale = maxOf(w / bw, h / bh)
        val zoom = 1.045f + 0.025f * ((sin(p.toDouble()).toFloat() + 1f) / 2f)
        val scale = baseScale * zoom
        val drawW = bw * scale
        val drawH = bh * scale
        val driftX = (drawW - w) * (0.5f + 0.16f * sin((p * 0.7f).toDouble()).toFloat())
        val driftY = (drawH - h) * (0.5f + 0.10f * sin((p * 0.55f + 1.2f).toDouble()).toFloat())
        val src = Rect(0, 0, b.width, b.height)
        val dst = RectF(-driftX, -driftY, -driftX + drawW, -driftY + drawH)

        paint.alpha = alphaValue
        paint.shader = null
        canvas.drawBitmap(b, src, dst, paint)

        // Dark cinematic veil keeps diary text readable without hiding the photo.
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0x33000000, 0x12000000, 0x66000000),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
    }

    private fun loadPhoto() {
        val url = PHOTO_URLS[preset] ?: return
        val cacheDir = File(context.filesDir, "aximo_wallpapers").apply { mkdirs() }
        val file = File(cacheDir, "$preset.jpg")

        if (file.exists() && file.length() > 50_000L) {
            executor.execute {
                val loaded = BitmapFactory.decodeFile(file.absolutePath)
                main.post {
                    bitmap = loaded
                    invalidateSelf()
                }
            }
            return
        }

        executor.execute {
            runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 9000
                connection.readTimeout = 12000
                connection.instanceFollowRedirects = true
                connection.doInput = true
                connection.connect()
                if (connection.responseCode in 200..299) {
                    connection.inputStream.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                connection.disconnect()
                BitmapFactory.decodeFile(file.absolutePath)
            }.onSuccess { loaded ->
                main.post {
                    bitmap = loaded
                    invalidateSelf()
                }
            }
        }
    }

    override fun setAlpha(alpha: Int) { alphaValue = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    fun stop() {
        animator.cancel()
        executor.shutdownNow()
        main.removeCallbacksAndMessages(null)
    }

    companion object {
        // Stable Unsplash photo URLs; downloaded once and then used offline.
        private val PHOTO_URLS = mapOf(
            "default" to "https://images.unsplash.com/photo-1519608487953-e999c86e7455?auto=format&fit=crop&w=1600&q=82",
            "mountains" to "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1600&q=82",
            "sea" to "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1600&q=82",
            "aurora" to "https://images.unsplash.com/photo-1483347756197-71ef80e95f73?auto=format&fit=crop&w=1600&q=82",
            "cosmos" to "https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=1600&q=82",
            "sunset" to "https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?auto=format&fit=crop&w=1600&q=82",
            "forest" to "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=1600&q=82",
            "rain" to "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?auto=format&fit=crop&w=1600&q=82",
            "ember" to "https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=1600&q=82",
            "lavender" to "https://images.unsplash.com/photo-1499002238440-d264edd596ec?auto=format&fit=crop&w=1600&q=82",
            "neon" to "https://images.unsplash.com/photo-1519608487953-e999c86e7455?auto=format&fit=crop&w=1600&q=82",
            "mist" to "https://images.unsplash.com/photo-1483347756197-71ef80e95f73?auto=format&fit=crop&w=1600&q=82",
            "stars" to "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?auto=format&fit=crop&w=1600&q=82",
            "city" to "https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?auto=format&fit=crop&w=1600&q=82"
        )
    }
}
