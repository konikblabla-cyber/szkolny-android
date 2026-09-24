package pl.szczodrzynski.edziennik.ui.aximo

import android.content.Context
import android.util.AttributeSet
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * Aximo's refresh container.
 *
 * Some diary screens contain their own nested vertical scrollers.  The timetable
 * must never let pull-to-refresh intercept those gestures, even when the nested
 * child is already at scroll position 0.
 */
class AximoSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SwipeRefreshLayout(context, attrs) {

    var blockRefreshGestures: Boolean = false

    override fun onInterceptTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (blockRefreshGestures) return false
        return super.onInterceptTouchEvent(ev)
    }

    override fun canChildScrollUp(): Boolean {
        if (blockRefreshGestures) return true
        return super.canChildScrollUp()
    }
}
