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
    private val handler=Handler(Looper.getMainLooper())
    private data class Rival(val view:BallView,val params:WindowManager.LayoutParams,var move:Runnable?=null)
    private val rivals=mutableListOf<Rival>()
    private var cfg=OverlayBus.config.copy()
    private var paused=false
    private var countTask:Runnable?=null

    override fun onCreate(){super.onCreate();OverlayBus.service=this;wm=getSystemService(Context.WINDOW_SERVICE) as WindowManager;createChannel();startForeground(77,notification());if(Settings.canDrawOverlays(this)) restartPattern()}
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{if(Settings.canDrawOverlays(this)){if(rivals.isEmpty())restartPattern() else applyConfig()};return START_STICKY}

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt().coerceAtLeast(1)
    private fun rnd(a:Int,b:Int)=if(a>=b)a else Random.nextInt(a,b+1)
    private fun rndLong(a:Long,b:Long)=if(a>=b)a else Random.nextLong(a,b+1)
    private fun normalized(){
        if(cfg.minBalls>cfg.maxBalls)cfg.maxBalls=cfg.minBalls
        if(cfg.minSizeDp>cfg.maxSizeDp)cfg.maxSizeDp=cfg.minSizeDp
        if(cfg.minStayMs>cfg.maxStayMs)cfg.maxStayMs=cfg.minStayMs
        if(cfg.minOpacity>cfg.maxOpacity)cfg.maxOpacity=cfg.minOpacity
        cfg.minOpacity=cfg.minOpacity.coerceIn(0,100);cfg.maxOpacity=cfg.maxOpacity.coerceIn(0,100)
    }
    private fun type()=if(Build.VERSION.SDK_INT>=26)WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
    private fun screenSize():Pair<Int,Int>{
        return if(Build.VERSION.SDK_INT>=30){val b=wm.currentWindowMetrics.bounds;Pair(b.width(),b.height())}else{@Suppress("DEPRECATION") val d=resources.displayMetrics;Pair(d.widthPixels,d.heightPixels)}
    }
    private fun makeRival(){
        val size=dp(rnd(cfg.minSizeDp,cfg.maxSizeDp));val p=WindowManager.LayoutParams(size,size,type(),WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START}
        lateinit var r:Rival
        val v=BallView(this){destroyRival(r)};r=Rival(v,p);randomize(r,true);wm.addView(v,p);rivals.add(r);scheduleMove(r)
    }
    private fun randomize(r:Rival,resize:Boolean){
        if(resize){val s=dp(rnd(cfg.minSizeDp,cfg.maxSizeDp));r.params.width=s;r.params.height=s}
        val (sw,sh)=screenSize();r.params.x=if(sw>r.params.width)rnd(0,sw-r.params.width) else 0;r.params.y=if(sh>r.params.height)rnd(0,sh-r.params.height) else 0
        r.view.opacityPercent=rnd(cfg.minOpacity,cfg.maxOpacity)
    }
    private fun scheduleMove(r:Rival){r.move?.let(handler::removeCallbacks);if(paused)return;val task=Runnable{if(rivals.contains(r)&&!paused){randomize(r,true);runCatching{wm.updateViewLayout(r.view,r.params)};scheduleMove(r)}};r.move=task;handler.postDelayed(task,rndLong(cfg.minStayMs,cfg.maxStayMs))}
    private fun destroyRival(r:Rival){r.move?.let(handler::removeCallbacks);rivals.remove(r);runCatching{wm.removeView(r.view)}}
    private fun setTargetCount(){val target=rnd(cfg.minBalls.coerceAtLeast(0),cfg.maxBalls.coerceAtLeast(0));while(rivals.size<target)makeRival();while(rivals.size>target)destroyRival(rivals.last())}
    private fun scheduleCount(){countTask?.let(handler::removeCallbacks);if(paused)return;val task=Runnable{if(!paused){setTargetCount();scheduleCount()}};countTask=task;handler.postDelayed(task,rndLong(5000,8000))}
    private fun restartPattern(){clearRivals();cfg=OverlayBus.config.copy();normalized();setTargetCount();scheduleCount()}
    private fun clearRivals(){countTask?.let(handler::removeCallbacks);countTask=null;rivals.toList().forEach(::destroyRival)}

    fun applyConfig(){cfg=OverlayBus.config.copy();normalized();setTargetCount();rivals.forEach{randomize(it,true);runCatching{wm.updateViewLayout(it.view,it.params)};scheduleMove(it)};scheduleCount()}
    fun newPattern(){restartPattern()}
    fun togglePause(){paused=!paused;if(paused){countTask?.let(handler::removeCallbacks);rivals.forEach{it.move?.let(handler::removeCallbacks)}}else{rivals.forEach(::scheduleMove);scheduleCount()}}

    private fun createChannel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("overlay","Ball Rival overlay",NotificationManager.IMPORTANCE_LOW))}
    private fun notification():Notification{val pi=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT);val b=if(Build.VERSION.SDK_INT>=26)Notification.Builder(this,"overlay")else @Suppress("DEPRECATION") Notification.Builder(this);return b.setContentTitle("Ball Rival is running").setContentText("Tap for settings").setSmallIcon(android.R.drawable.ic_menu_view).setContentIntent(pi).setOngoing(true).build()}
    override fun onDestroy(){clearRivals();OverlayBus.service=null;super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
}
