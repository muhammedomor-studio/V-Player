package studio.muhammedomor.vplayer.ui

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

class GestureOverlayView(
    context: Context,
    private val listener: GestureListener
) : View(context) {

    interface GestureListener {
        fun onDoubleTapLeft()
        fun onDoubleTapRight()
        fun onVerticalSwipeLeft(deltaY: Float)
        fun onVerticalSwipeRight(deltaY: Float)
    }

    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (e.x < width / 2f) {
                listener.onDoubleTapLeft()
            } else {
                listener.onDoubleTapRight()
            }
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (e1 == null) return false
            if (Math.abs(distanceY) > Math.abs(distanceX)) {
                if (e1.x < width / 2f) {
                    listener.onVerticalSwipeLeft(distanceY)
                } else {
                    listener.onVerticalSwipeRight(distanceY)
                }
                return true
            }
            return false
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        detector.onTouchEvent(event)
        return true
    }
}
