package com.ballrival.overlay

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import kotlin.random.Random

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val balls = ArrayList<BallEntry>()
    private var paused = false
    private var cfg = OverlayBus.config.copy()

    private data class BallEntry(
        val view: BallView,
        val params: WindowManager.LayoutParams,
        var teleport: Runnable? = null
    )

    override fun onCreate() {
        super.onCreate()
        OverlayBus.service = this
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createChannel()
        startForeground(77, notification())
        if (Settings.canDrawOverlays(this)) {
            normalize()
            reconcileCount(true)
            scheduleCountChange()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        applyConfig()
        return START_STICKY
    }

    private fun normalize() {
        if (cfg.minBalls > cfg.maxBalls) cfg.maxBalls = cfg.minBalls
        if (cfg.minSizeDp > cfg.maxSizeDp) cfg.maxSizeDp = cfg.minSizeDp
        if (cfg.minStayMs > cfg.maxStayMs) cfg.maxStayMs = cfg.minStayMs
        if (cfg.minOpacity > cfg.maxOpacity) cfg.maxOpacity = cfg.minOpacity
        cfg.minBalls = cfg.minBalls.coerceIn(0, 200)
        cfg.maxBalls = cfg.maxBalls.coerceIn(cfg.minBalls, 200)
        cfg.minOpacity = cfg.minOpacity.coerceIn(0, 100)
        cfg.maxOpacity = cfg.maxOpacity.coerceIn(cfg.minOpacity, 100)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt().coerceAtLeast(1)
    private fun rndInt(a: Int, b: Int) = if (a >= b) a else Random.nextInt(a, b + 1)
    private fun rndLong(a: Long, b: Long) = if (a >= b) a else Random.nextLong(a, b + 1)

    private fun overlayType() = if (Build.VERSION.SDK_INT >= 26)
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun makeParams(sizePx: Int): WindowManager.LayoutParams {
        val metrics = resources.displayMetrics
        val maxX = (metrics.widthPixels - sizePx).coerceAtLeast(0)
        val maxY = (metrics.heightPixels - sizePx).coerceAtLeast(0)
        return WindowManager.LayoutParams(
            sizePx, sizePx, overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (maxX == 0) 0 else Random.nextInt(maxX + 1)
            y = if (maxY == 0) 0 else Random.nextInt(maxY + 1)
        }
    }

    private fun addBall() {
        if (paused || balls.size >= 200) return
        val size = dp(rndInt(cfg.minSizeDp, cfg.maxSizeDp))
        val opacity = rndInt(cfg.minOpacity, cfg.maxOpacity)
        lateinit var entry: BallEntry
        val view = BallView(this, opacity) { removeBall(entry) }
        entry = BallEntry(view, makeParams(size))
        balls.add(entry)
        wm.addView(view, entry.params)
        scheduleTeleport(entry)
    }

    private fun removeBall(entry: BallEntry) {
        entry.teleport?.let(handler::removeCallbacks)
        if (balls.remove(entry)) runCatching { wm.removeView(entry.view) }
    }

    private fun teleport(entry: BallEntry) {
        if (paused || !balls.contains(entry)) return
        val newSize = dp(rndInt(cfg.minSizeDp, cfg.maxSizeDp))
        val p = makeParams(newSize)
        entry.params.width = p.width
        entry.params.height = p.height
        entry.params.x = p.x
        entry.params.y = p.y
        runCatching { wm.updateViewLayout(entry.view, entry.params) }
        // Opacity is randomized without animation by replacing only this small view.
        val index = balls.indexOf(entry)
        if (index >= 0) {
            entry.teleport?.let(handler::removeCallbacks)
            runCatching { wm.removeView(entry.view) }
            balls.removeAt(index)
            val opacity = rndInt(cfg.minOpacity, cfg.maxOpacity)
            lateinit var replacement: BallEntry
            val newView = BallView(this, opacity) { removeBall(replacement) }
            replacement = BallEntry(newView, entry.params)
            balls.add(index, replacement)
            wm.addView(newView, replacement.params)
            scheduleTeleport(replacement)
        }
    }

    private fun scheduleTeleport(entry: BallEntry) {
        val r = Runnable { teleport(entry) }
        entry.teleport = r
        handler.postDelayed(r, rndLong(cfg.minStayMs, cfg.maxStayMs))
    }

    private fun reconcileCount(force: Boolean = false) {
        if (paused && !force) return
        val target = rndInt(cfg.minBalls, cfg.maxBalls)
        while (balls.size < target) addBall()
        while (balls.size > target) removeBall(balls.last())
    }

    private val countChange = object : Runnable {
        override fun run() {
            if (!paused) reconcileCount()
            if (!paused) scheduleCountChange()
        }
    }

    private fun scheduleCountChange() {
        handler.removeCallbacks(countChange)
        if (!paused) handler.postDelayed(countChange, rndLong(5000, 8000))
    }

    fun applyConfig() {
        cfg = OverlayBus.config.copy()
        normalize()
        if (!paused) {
            reconcileCount(true)
            scheduleCountChange()
        }
    }

    fun newPattern() {
        clearBalls()
        if (!paused) {
            reconcileCount(true)
            scheduleCountChange()
        }
    }

    fun togglePause() {
        paused = !paused
        if (paused) {
            handler.removeCallbacks(countChange)
            balls.forEach { it.teleport?.let(handler::removeCallbacks) }
        } else {
            balls.forEach(::scheduleTeleport)
            reconcileCount(true)
            scheduleCountChange()
        }
    }

    private fun clearBalls() {
        val copy = balls.toList()
        copy.forEach(::removeBall)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("overlay", "Ball Rival overlay", NotificationManager.IMPORTANCE_LOW))
    }

    private fun notification(): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "overlay") else @Suppress("DEPRECATION") Notification.Builder(this)
        return b.setContentTitle("Ball Rival is running").setContentText("Tap a ball to destroy it").setSmallIcon(android.R.drawable.ic_menu_view).setContentIntent(pi).setOngoing(true).build()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        clearBalls()
        OverlayBus.service = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
