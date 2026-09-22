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
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.sizeDp
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.ui.aximo.AximoAppearanceStyle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class AximoBottomNavigation @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private data class Item(val target: NavTarget, val label: String)

    private val allItems = listOf(
        Item(NavTarget.HOME, "Start"),
        Item(NavTarget.TIMETABLE, "Plan"),
        Item(NavTarget.MESSAGES, "Powiadomienia"),
        Item(NavTarget.GRADES, "Oceny"),
        Item(NavTarget.HOMEWORK, "Zadania"),
        Item(NavTarget.ATTENDANCE, "Frekwencja"),
        Item(NavTarget.AGENDA, "Kalendarz"),
        Item(NavTarget.NOTES, "Notatki"),
        Item(NavTarget.TEACHERS, "Nauczyciele"),
        Item(NavTarget.SETTINGS, "Ustawienia"),
    )

    private val bottomItems = listOf(
        allItems[0],
        allItems[1],
        allItems[2],
        Item(NavTarget.MORE, "Więcej")
    )

    private val center = TextView(context)
    private val menuViews = mutableListOf<TextView>()
    private var open = false
    private var selected = -1

    private val appearance: AximoAppearanceStyle
        get() = AximoAppearanceStyle.fromOrdinal(
            (context.applicationContext as App).config.ui.aximoAppearanceStyle
        )

    init {
        clipChildren = false
        clipToPadding = false
        setBackgroundColor(Color.TRANSPARENT)

        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(5), dp(8), dp(5))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.argb(238, 4, 10, 30), Color.argb(246, 10, 13, 43), Color.argb(238, 4, 10, 30))
            ).apply {
                cornerRadius = dp(25).toFloat()
                setStroke(dp(1), Color.argb(210, 37, 66, 132))
            }
            elevation = 12f
        }

        bottomItems.forEachIndexed { index, item ->
            val button = createItemView(item, 8.5f)
            if (index == 0) applyActive(button)
            button.setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                (context as? MainActivity)?.navigate(navTarget = item.target)
            }
            bar.addView(button, LinearLayout.LayoutParams(0, dp(58), 1f))
        }

        addView(
            bar,
            LayoutParams(LayoutParams.MATCH_PARENT, dp(68), Gravity.BOTTOM).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
                bottomMargin = dp(3)
            }
        )

        center.visibility = View.INVISIBLE

        allItems.forEach { item ->
            val view = createItemView(item, 9f).apply {
                alpha = 0f
                scaleX = .55f
                scaleY = .55f
                elevation = 18f
                visibility = View.INVISIBLE
                background = roundedBackground(appearance.surfaceAlt, appearance.accentSoft, 1, 20)
            }
            menuViews += view
            addView(view, LayoutParams(dp(78), dp(60)))
        }

        menuViews.forEachIndexed { index, view ->
            view.setOnClickListener { navigate(index) }
        }
    }

    private fun createItemView(item: Item, textSize: Float): TextView =
        TextView(context).apply {
            gravity = Gravity.CENTER
            text = item.label
            this.textSize = textSize
            setTextColor(appearance.text)
            setPadding(dp(2), dp(2), dp(2), dp(2))
            background = roundedBackground(Color.TRANSPARENT, Color.TRANSPARENT, 0, 18)
            contentDescription = item.label

            item.target.icon?.let { icon ->
                val drawable = IconicsDrawable(context).apply {
                    this.icon = icon
                    sizeDp = 20
                    setTint(appearance.text)
                }
                setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null)
                compoundDrawablePadding = dp(1)
            }
        }

    fun setActiveTarget(target: NavTarget?) {
        bottomItems.forEachIndexed { index, item ->
            val view = (getChildAt(0) as? LinearLayout)?.getChildAt(index) as? TextView
                ?: return@forEachIndexed
            if (item.target == target) {
                applyActive(view)
            } else {
                view.background = roundedBackground(Color.TRANSPARENT, Color.TRANSPARENT, 0, 18)
                view.setTextColor(appearance.text)
                view.compoundDrawables.forEach { it?.setTint(appearance.text) }
                view.elevation = 0f
            }
        }
    }

    private fun applyActive(view: TextView) {
        view.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.argb(245, 74, 35, 150), Color.argb(225, 49, 18, 111))
        ).apply {
            cornerRadius = dp(20).toFloat()
            setStroke(dp(1), Color.argb(180, 140, 83, 255))
        }
        view.setTextColor(Color.WHITE)
        view.compoundDrawables.forEach { it?.setTint(Color.WHITE) }
        view.elevation = 8f
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
            view.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay((i * 10).toLong()).setDuration(160).start()
        }
        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    private fun closeMenu() {
        open = false
        selected = -1
        menuViews.forEach { view ->
            view.animate().alpha(0f).scaleX(.55f).scaleY(.55f).setDuration(90)
                .withEndAction { if (!open) view.visibility = View.INVISIBLE }.start()
        }
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
        setSelected(if (bestDistance <= dp(72)) best else -1)
    }

    private fun setSelected(index: Int) {
        if (selected == index) return
        selected = index
        menuViews.forEachIndexed { i, view ->
            val active = i == index
            view.background = roundedBackground(
                if (active) appearance.accent else appearance.surfaceAlt,
                appearance.accentSoft, 1, 20
            )
            view.setTextColor(if (active) Color.WHITE else appearance.text)
            view.compoundDrawables.forEach { it?.setTint(if (active) Color.WHITE else appearance.text) }
            view.scaleX = if (active) 1.12f else 1f
            view.scaleY = if (active) 1.12f else 1f
        }
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

    private fun roundedBackground(fill: Int, stroke: Int, width: Int, radius: Int) =
        GradientDrawable().apply {
            cornerRadius = dp(radius).toFloat()
            setColor(fill)
            if (width > 0) setStroke(dp(width), stroke)
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}