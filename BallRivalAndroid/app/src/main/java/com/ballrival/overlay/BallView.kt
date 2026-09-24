package com.ballrival.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View

class BallView(
    context: Context,
    opacityPercent: Int,
    private val onDestroyed: () -> Unit
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(100, 149, 237)
        alpha = (255f * opacityPercent.coerceIn(0, 100) / 100f).toInt()
    }

    init {
        setWillNotDraw(false)
        isClickable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val r = minOf(width, height) / 2f
        canvas.drawCircle(width / 2f, height / 2f, r, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val cx = width / 2f
            val cy = height / 2f
            val dx = event.x - cx
            val dy = event.y - cy
            val r = minOf(width, height) / 2f
            if (dx * dx + dy * dy <= r * r) {
                performClick()
                onDestroyed()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
