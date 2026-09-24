package pl.szczodrzynski.edziennik.ui.main

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.MotionEvent
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

    private val bottomItems = listOf(
        Item(NavTarget.HOME, "Start"),
        Item(NavTarget.TIMETABLE, "Plan"),
        Item(NavTarget.NOTIFICATIONS, "Powiadomienia"),
        Item(NavTarget.MORE, "Więcej"),
    )

    // Every radial item has a matching setting below. No decorative/dead shortcuts.
    private val menuItems = listOf(
        Item(NavTarget.GRADES, "Oceny"),
        Item(NavTarget.HOMEWORK, "Zadania"),
        Item(NavTarget.ATTENDANCE, "Frekwencja"),
        Item(NavTarget.TIMETABLE, "Plan"),
        Item(NavTarget.MESSAGES, "Wiadomości"),
        Item(NavTarget.SETTINGS, "Ustawienia"),
    )

    private val menuViews = mutableListOf<TextView>()
    private var open = false
    private var selected = -1
    private var downX = 0f
    private var downY = 0f
    private val settingsPrefs by lazy { context.getSharedPreferences("aximo_settings", Context.MODE_PRIVATE) }

    private val appearance: AximoAppearanceStyle
        get() = AximoAppearanceStyle.fromOrdinal(
            (context.applicationContext as App).config.ui.aximoAppearanceStyle
        )

    init {
        clipChildren = false
        clipToPadding = false
        setBackgroundColor(Color.TRANSPARENT)
        isClickable = false
        isFocusable = false

        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
            clipToPadding = false
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.argb(245, 4, 9, 27), Color.argb(250, 11, 15, 45), Color.argb(245, 4, 9, 27))
            ).apply {
                cornerRadius = dp(27).toFloat()
                setStroke(dp(1), Color.argb(150, 120, 86, 190))
            }
            elevation = 10f
            alpha = .96f
        }

        bottomItems.forEach { item ->
            val button = createItemView(item)
            button.setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                navigateTo(item.target)
            }
            bar.addView(button, LinearLayout.LayoutParams(0, dp(54), 1f).apply {
                marginStart = dp(2)
                marginEnd = dp(2)
            })
        }

        addView(
            bar,
            LayoutParams(LayoutParams.MATCH_PARENT, dp(66), Gravity.BOTTOM).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
                bottomMargin = dp(4)
            }
        )

        // Keep the custom bar above every fragment and synchronize its selected
        // state after the first layout pass. This avoids a dead-looking Start
        // tab when MainActivity restores a fragment during launch.
        post {
            (context as? MainActivity)?.let { setActiveTarget(it.navTarget) }
            refreshSettings()
        }

        // Long-press the bottom bar to reveal the radial Aximo menu.
        bar.isClickable = true
        bar.isLongClickable = true
        bar.setOnLongClickListener {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            if (open) closeMenu() else openMenu()
            true
        }
        bar.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    false
                }
                MotionEvent.ACTION_UP -> {
                    val moved = sqrt((event.x - downX) * (event.x - downX) + (event.y - downY) * (event.y - downY))
                    if (moved < dp(12) && !open) {
                        bar.animate().scaleX(.985f).scaleY(.985f).setDuration(55).withEndAction {
                            bar.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        }.start()
                    }
                    false
                }
            }
            false
        }

        menuItems.forEach { item ->
            val view = createItemView(item).apply {
                alpha = 0f
                scaleX = .55f
                scaleY = .55f
                visibility = View.INVISIBLE
                background = roundedBackground(appearance.surfaceAlt, appearance.accentSoft, 1, 20)
                setOnClickListener { navigateTo(item.target); closeMenu() }
            }
            menuViews += view
            addView(view, LayoutParams(dp(92), dp(60)))
        }
    }

    private fun createItemView(item: Item): TextView =
        TextView(context).apply {
            gravity = Gravity.CENTER
            text = item.label
            textSize = 10f
            setTextColor(appearance.text)
            setPadding(dp(2), dp(2), dp(2), dp(2))
            background = roundedBackground(Color.TRANSPARENT, Color.TRANSPARENT, 0, 20)
            contentDescription = item.label
            item.target.icon?.let { icon ->
                val drawable = IconicsDrawable(context).apply {
                    this.icon = icon
                    sizeDp = 19
                    setTint(appearance.text)
                }
                setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null)
                compoundDrawablePadding = dp(1)
            }
        }

    fun refreshSettings() {
        val showBottom = settingsPrefs.getBoolean("nav_bottom", true)
        visibility = if (showBottom) View.VISIBLE else View.GONE
        if (!showBottom) closeMenu()
        val bar = getChildAt(0) as? LinearLayout ?: return
        bottomItems.forEachIndexed { index, item ->
            val enabled = item.target != NavTarget.TIMETABLE || settingsPrefs.getBoolean("nav_timetable", true)
            bar.getChildAt(index).visibility = if (enabled) View.VISIBLE else View.GONE
        }
        val radialKeys = listOf("nav_grades", "nav_homework", "nav_attendance", "nav_timetable", "nav_messages", "nav_settings")
        menuItems.forEachIndexed { index, _ ->
            val enabled = radialKeys.getOrNull(index)?.let { settingsPrefs.getBoolean(it, true) } ?: true
            menuViews[index].alpha = if (enabled) 1f else 0f
            if (!open || !enabled) menuViews[index].visibility = View.INVISIBLE
        }
    }

    fun setActiveTarget(target: NavTarget?) {
        val bar = getChildAt(0) as? LinearLayout ?: return
        bottomItems.forEachIndexed { index, item ->
            val view = bar.getChildAt(index) as? TextView ?: return@forEachIndexed
            if (item.target == target) {
                applyActive(view)
            } else {
                view.background = roundedBackground(Color.TRANSPARENT, Color.TRANSPARENT, 0, 20)
                view.setTextColor(appearance.text)
                view.compoundDrawables.forEach { it?.setTint(appearance.text) }
                view.elevation = 0f
            }
        }
    }

    private fun applyActive(view: TextView) {
        view.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.argb(248, 78, 39, 158), Color.argb(232, 47, 20, 108))
        ).apply {
            cornerRadius = dp(20).toFloat()
            setStroke(dp(1), Color.argb(190, 146, 90, 255))
        }
        view.setTextColor(Color.WHITE)
        view.compoundDrawables.forEach { it?.setTint(Color.WHITE) }
        view.elevation = 8f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        post { positionMenu() }
    }

    private fun positionMenu() {
        if (width <= 0 || height <= 0) return
        val cx = width / 2f
        val cy = height - dp(36).toFloat()
        val radius = dp(108).toFloat()
        menuViews.forEachIndexed { i, view ->
            val angle = Math.toRadians(205.0 + i * 24.0)
            view.x = (cx + cos(angle) * radius - view.width / 2f).toFloat()
            view.y = (cy + sin(angle) * radius - view.height / 2f).toFloat()
        }
    }

    private fun openMenu() {
        if (open) return
        if (!settingsPrefs.getBoolean("nav_radial", true)) return
        open = true
        menuViews.forEachIndexed { i, view ->
            val key = listOf("nav_grades", "nav_homework", "nav_attendance", "nav_timetable", "nav_messages", "nav_settings").getOrNull(i)
            if (key != null && !settingsPrefs.getBoolean(key, true)) return@forEachIndexed
            view.visibility = View.VISIBLE
            view.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(i * 18L).setDuration(170).start()
        }
    }

    private fun closeMenu() {
        open = false
        menuViews.forEach { view ->
            view.animate().alpha(0f).scaleX(.55f).scaleY(.55f).setDuration(100)
                .withEndAction { if (!open) view.visibility = View.INVISIBLE }.start()
        }
    }

    private fun navigateTo(target: NavTarget) {
        val activity = context as? MainActivity ?: return
        closeMenu()
        activity.navigate(navTarget = target, skipBeforeNavigate = true)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun roundedBackground(fill: Int, stroke: Int, width: Int, radius: Int) =
        GradientDrawable().apply {
            cornerRadius = dp(radius).toFloat()
            setColor(fill)
            if (width > 0) setStroke(dp(width), stroke)
        }
}
