package pl.szczodrzynski.edziennik.ui.grades

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors

class GradesStatisticsTrendView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Path()
    private var values: List<Float> = emptyList()
    private var labels: List<String> = emptyList()

    fun setValues(values: List<Float>, labels: List<String>) {
        this.values = values
        this.labels = labels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return
        val left = 32f
        val right = width - 24f
        val top = 24f
        val bottom = height - 28f
        val step = if (values.size == 1) 0f else (right - left) / (values.size - 1)

        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        paint.color = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
        line.reset()

        values.forEachIndexed { index, value ->
            val x = left + index * step
            val y = bottom - ((value.coerceIn(1f, 6f) - 1f) / 5f) * (bottom - top)
            if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        canvas.drawPath(line, paint)

        paint.style = Paint.Style.FILL
        values.forEachIndexed { index, value ->
            val x = left + index * step
            val y = bottom - ((value.coerceIn(1f, 6f) - 1f) / 5f) * (bottom - top)
            canvas.drawCircle(x, y, 5f, paint)
            if (index < labels.size) {
                paint.textSize = 22f
                canvas.drawText(labels[index].take(6), x - 18f, height - 6f, paint)
            }
        }
    }
}
