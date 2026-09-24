package com.ballrival.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class BallView(context: Context, private val onDestroyBall: () -> Unit) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(100,149,237) }
    var opacityPercent: Int = 100
        set(value) { field = value.coerceIn(0,100); invalidate() }

    init { setWillNotDraw(false) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.alpha = (255f * opacityPercent / 100f).toInt().coerceIn(0,255)
        val r = min(width,height) / 2f
        canvas.drawCircle(width/2f,height/2f,r,paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cx=width/2f; val cy=height/2f
        val dx=event.x-cx; val dy=event.y-cy; val r=min(width,height)/2f
        val inside = dx*dx + dy*dy <= r*r
        if (!inside) return false
        if (event.action == MotionEvent.ACTION_UP) { performClick(); onDestroyBall() }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}
