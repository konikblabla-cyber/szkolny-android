package pl.szczodrzynski.edziennik.ui.main

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class AximoBottomNavigation @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private data class Item(val target: NavTarget, val label: String, val icon: String)

    private val allItems = listOf(
        Item(NavTarget.HOME, "Start", "home-outline"),
        Item(NavTarget.TIMETABLE, "Plan", "calendar-clock-outline"),
        Item(NavTarget.GRADES, "Oceny", "star-outline"),
        Item(NavTarget.HOMEWORK, "Zadania", "clipboard-check-outline"),
        Item(NavTarget.MESSAGES, "Wiadomości", "email-outline"),
        Item(NavTarget.ATTENDANCE, "Frekwencja", "chart-donut"),
        Item(NavTarget.AGENDA, "Kalendarz", "calendar-month-outline"),
        Item(NavTarget.NOTES, "Notatki", "note-text-outline"),
        Item(NavTarget.TEACHERS, "Nauczyciele", "account-school-outline"),
        Item(NavTarget.SETTINGS, "Ustawienia", "cog-outline"),
    )

    private val quickItems = listOf(
        allItems[0], // Start
        allItems[1], // Plan
        allItems[2], // Oceny
        allItems[3], // Zadania
        allItems[9], // Ustawienia — zawsze widoczne
    )
    private val center = TextView(context)
    private val menuViews = mutableListOf<TextView>()
    private var open = false
    private var selected = -1
    private var holdRunnable: Runnable? = null

    init {
        clipChildren = false
        clipToPadding = false
        setBackgroundColor(Color.TRANSPARENT)

        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(5), dp(8), dp(5))
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(0xFF1C1728.toInt(), 0xFF211A30.toInt(), 0xFF18242A.toInt())).apply { cornerRadius = dp(30).toFloat(); setStroke(dp(1), 0xFF3D3454.toInt()) }
            elevation = 10f
        }

        quickItems.forEachIndexed { index, item ->
            val button = TextView(context).apply {
                gravity = Gravity.CENTER
                text = iconGlyph(item.icon) + "\n" + item.label
                textSize = if (index == 0) 10.5f else 9.5f
                setTextColor(0xFFEDE8F6.toInt())
                setPadding(dp(4), dp(4), dp(4), dp(4))
                background = roundedBackground(Color.TRANSPARENT, Color.TRANSPARENT, 0, 18)
                contentDescription = item.label
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    (context as? MainActivity)?.navigate(navTarget = item.target)
                }
            }
            bar.addView(button, LinearLayout.LayoutParams(0, dp(58), 1f))
        }

        addView(
            bar,
            LayoutParams(LayoutParams.MATCH_PARENT, dp(68), Gravity.BOTTOM).apply {
                marginStart = dp(6)
                marginEnd = dp(6)
                bottomMargin = dp(4)
            }
        )

        center.apply {
            text = "A"
            textSize = 22f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            elevation = 24f
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFF7657FF.toInt(), 0xFFE84F9B.toInt(), 0xFF22B8A7.toInt())).apply { shape = GradientDrawable.OVAL; setStroke(dp(2), 0xFFFFFFFF.toInt()) }
            contentDescription = "Aximo — Start. Przytrzymaj, aby otworzyć pełne menu."
        }
        addView(
            center,
            LayoutParams(dp(58), dp(58), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                bottomMargin = dp(9)
            }
        )

        allItems.forEachIndexed { _, item ->
            val view = TextView(context).apply {
                text = iconGlyph(item.icon) + "\n" + item.label
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(0xFFE2DCEC.toInt())
                alpha = 0f
                scaleX = .55f
                scaleY = .55f
                elevation = 18f
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFF252034.toInt(), 0xFF1A2630.toInt())).apply { cornerRadius = dp(22).toFloat(); setStroke(dp(1), 0xFF403654.toInt()) }
                contentDescription = item.label
                visibility = View.INVISIBLE
            }
            menuViews += view
            addView(view, LayoutParams(dp(66), dp(58)))
        }

        menuViews.forEachIndexed { index, view ->
            view.setOnClickListener { navigate(index) }
        }

        center.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    holdRunnable = Runnable { openMenu() }.also { postDelayed(it, 320L) }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    updateSelection(event.rawX, event.rawY)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    holdRunnable?.let { removeCallbacks(it) }
                    holdRunnable = null
                    if (open) {
                        if (selected >= 0) navigate(selected) else closeMenu()
                    } else {
                        (context as? MainActivity)?.navigate(navTarget = NavTarget.HOME)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    holdRunnable?.let { removeCallbacks(it) }
                    holdRunnable = null
                    closeMenu()
                    true
                }
                else -> true
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) positionMenu()
    }

    private fun positionMenu() {
        val cx = width / 2f
        val cy = height - dp(38).toFloat()
        val radius = dp(122).toFloat()
        menuViews.forEachIndexed { i, view ->
            view.post {
                val angle = Math.toRadians(202.0 + i * 31.8)
                view.x = cx + cos(angle).toFloat() * radius - view.width / 2f
                view.y = cy + sin(angle).toFloat() * radius - view.height / 2f
            }
        }
    }

    private fun openMenu() {
        if (open) return
        open = true
        selected = -1
        menuViews.forEachIndexed { i, view ->
            view.visibility = View.VISIBLE
            view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay((i * 10).toLong())
                .setDuration(160)
                .start()
        }
        center.animate().rotation(45f).scaleX(1.06f).scaleY(1.06f).setDuration(160).start()
        center.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    private fun closeMenu() {
        open = false
        selected = -1
        menuViews.forEach { view ->
            view.animate().alpha(0f).scaleX(.55f).scaleY(.55f).setDuration(90)
                .withEndAction { if (!open) view.visibility = View.INVISIBLE }.start()
        }
        center.animate().rotation(0f).scaleX(1f).scaleY(1f).setDuration(110).start()
    }

    private fun updateSelection(x: Float, y: Float) {
        if (!open) return
        val location = IntArray(2)
        getLocationOnScreen(location)
        val localX = x - location[0]
        val localY = y - location[1]
        var best = -1
        var bestDistance = Double.MAX_VALUE

        menuViews.forEachIndexed { index, view ->
            val itemX = view.x + view.width / 2f
            val itemY = view.y + view.height / 2f
            val dx = localX - itemX
            val dy = localY - itemY
            val distance = sqrt((dx * dx + dy * dy).toDouble())
            if (distance < bestDistance) {
                bestDistance = distance
                best = index
            }
        }
        setSelected(if (bestDistance <= dp(70)) best else -1)
    }

    private fun setSelected(index: Int) {
        if (selected == index) return
        selected = index
        menuViews.forEachIndexed { i, view ->
            val active = i == index
            view.background = roundedBackground(
                if (active) 0xFF7657FF.toInt() else 0xFF252034.toInt(),
                if (active) 0xFFFFA9D0.toInt() else 0xFF403654.toInt(),
                1,
                22
            )
            view.setTextColor(if (active) Color.WHITE else 0xFFEDE8F6.toInt())
            view.scaleX = if (active) 1.12f else 1f
            view.scaleY = if (active) 1.12f else 1f
        }
        if (index >= 0) menuViews[index].performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    private fun navigate(index: Int) {
        if (index !in allItems.indices) {
            closeMenu()
            return
        }
        val target = allItems[index].target
        closeMenu()
        (context as? MainActivity)?.navigate(navTarget = target)
    }

    private fun iconGlyph(name: String): String = when (name) {
        "home-outline" -> "⌂"
        "calendar-clock-outline" -> "▦"
        "star-outline" -> "☆"
        "clipboard-check-outline" -> "☑"
        "email-outline" -> "✉"
        "chart-donut" -> "◔"
        "calendar-month-outline" -> "◫"
        "note-text-outline" -> "✎"
        "account-school-outline" -> "♙"
        "cog-outline" -> "⚙"
        else -> "•"
    }

    private fun circleBackground(fill: Int, stroke: Int, width: Int) =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
            setStroke(dp(width), stroke)
        }

    private fun roundedBackground(fill: Int, stroke: Int, width: Int, radius: Int) =
        GradientDrawable().apply {
            cornerRadius = dp(radius).toFloat()
            setColor(fill)
            if (width > 0) setStroke(dp(width), stroke)
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
