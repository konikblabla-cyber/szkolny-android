package pl.szczodrzynski.edziennik.ui.aximo

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * Aximo refresh container.
 *
 * Pull-to-refresh is allowed only when the scrollable view receiving the
 * gesture is already at the top. Normal scrolling always wins otherwise.
 */
class AximoSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SwipeRefreshLayout(context, attrs) {

    var blockRefreshGestures: Boolean = false

    private var downX = 0f
    private var downY = 0f
    private var activeScrollableChild: View? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (blockRefreshGestures || !isEnabled) return false

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                activeScrollableChild = findTouchedScrollable(this, ev.x, ev.y)
                return super.onInterceptTouchEvent(ev)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - downX
                val dy = ev.y - downY

                // Upward finger movement is normal scrolling, never refresh.
                if (dy < 0f && kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
                    return false
                }

                // While the touched descendant can still scroll upward, keep
                // the gesture with that descendant. Refresh becomes possible
                // only after it reaches the absolute top.
                if (dy > 0f && kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
                    if (activeScrollableChild?.canScrollVertically(-1) == true) {
                        return false
                    }
                }
            }
        }

        return super.onInterceptTouchEvent(ev)
    }

    override fun canChildScrollUp(): Boolean {
        if (blockRefreshGestures) return true
        return activeScrollableChild?.canScrollVertically(-1) == true || super.canChildScrollUp()
    }

    private fun findTouchedScrollable(parent: ViewGroup, x: Float, y: Float): View? {
        for (i in parent.childCount - 1 downTo 0) {
            val child = parent.getChildAt(i)
            if (!child.isShown) continue

            val location = Rect()
            child.getHitRect(location)
            if (!location.contains(x.toInt(), y.toInt())) continue

            if (child.canScrollVertically(-1)) return child

            if (child is ViewGroup) {
                val childX = x - child.left
                val childY = y - child.top
                findTouchedScrollable(child, childX, childY)?.let { return it }
            }
        }
        return null
    }
}
