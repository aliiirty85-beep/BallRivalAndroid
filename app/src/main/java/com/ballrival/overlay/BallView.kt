package com.ballrival.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import kotlin.math.max
import kotlin.random.Random

class BallView(context: Context) : View(context) {
    data class Ball(var x: Float, var y: Float, var r: Float, var due: Long)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val balls = ArrayList<Ball>()
    private var cfg = OverlayBus.config.copy()
    private var paused = false
    private var nextCountChange = 0L
    private val tick = object : Runnable {
        override fun run() {
            if (!paused) updateState()
            postDelayed(this, 200L) // only 5 checks/sec; no animation loop
        }
    }

    init { setWillNotDraw(false); post(tick) }

    fun applyConfig(c: OverlayConfig) {
        cfg = c.copy()
        normalize()
        reconcileCount(true)
        invalidate()
    }
    fun newPattern() { balls.clear(); reconcileCount(true); invalidate() }
    fun togglePause() { paused = !paused }

    private fun normalize() {
        if (cfg.minBalls > cfg.maxBalls) cfg.maxBalls = cfg.minBalls
        if (cfg.minSizeDp > cfg.maxSizeDp) cfg.maxSizeDp = cfg.minSizeDp
        if (cfg.minStayMs > cfg.maxStayMs) cfg.maxStayMs = cfg.minStayMs
        cfg.opacity = cfg.opacity.coerceIn(1,100)
    }
    private fun dp(v:Int) = v * resources.displayMetrics.density
    private fun rndInt(a:Int,b:Int)= if(a>=b) a else Random.nextInt(a,b+1)
    private fun rndLong(a:Long,b:Long)= if(a>=b) a else Random.nextLong(a,b+1)

    private fun makeBall(now:Long): Ball {
        val diameter = dp(rndInt(cfg.minSizeDp,cfg.maxSizeDp))
        val r = diameter/2f
        val w = max(width.toFloat(), r*2+1)
        val h = max(height.toFloat(), r*2+1)
        val x = if(w <= r*2) w/2 else Random.nextFloat()*(w-r*2)+r
        val y = if(h <= r*2) h/2 else Random.nextFloat()*(h-r*2)+r
        return Ball(x,y,r,now+rndLong(cfg.minStayMs,cfg.maxStayMs))
    }
    private fun teleport(b:Ball, now:Long) {
        val n=makeBall(now); b.x=n.x; b.y=n.y; b.r=n.r; b.due=n.due
    }
    private fun reconcileCount(force:Boolean=false) {
        if(width<=0 || height<=0) return
        val now=SystemClock.uptimeMillis()
        if(!force && now < nextCountChange) return
        val target=rndInt(cfg.minBalls.coerceAtLeast(0),cfg.maxBalls.coerceAtLeast(0))
        while(balls.size<target) balls.add(makeBall(now))
        while(balls.size>target) balls.removeAt(balls.lastIndex)
        nextCountChange=now+rndLong(5000,8000)
    }
    private fun updateState() {
        if(width<=0 || height<=0) return
        val now=SystemClock.uptimeMillis(); var changed=false
        for(b in balls) if(now>=b.due){ teleport(b,now); changed=true }
        val before=balls.size; reconcileCount(false); if(before!=balls.size) changed=true
        if(changed) invalidate()
    }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int){ super.onSizeChanged(w,h,oldw,oldh); newPattern() }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        paint.color=Color.rgb(100,149,237)
        paint.alpha=(255*cfg.opacity/100f).toInt().coerceIn(1,255)
        for(b in balls) canvas.drawCircle(b.x,b.y,b.r,paint)
    }
    override fun onDetachedFromWindow(){ removeCallbacks(tick); super.onDetachedFromWindow() }
}
