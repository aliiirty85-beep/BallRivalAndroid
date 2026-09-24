package com.ballrival.overlay

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var view: BallView? = null
    override fun onCreate() {
        super.onCreate(); OverlayBus.service=this; createChannel(); startForeground(77, notification())
        if(Settings.canDrawOverlays(this)) showOverlay()
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(view==null && Settings.canDrawOverlays(this)) showOverlay() else view?.applyConfig(OverlayBus.config)
        return START_STICKY
    }
    private fun showOverlay(){
        wm=getSystemService(Context.WINDOW_SERVICE) as WindowManager
        view=BallView(this)
        val type=if(Build.VERSION.SDK_INT>=26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        val p=WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,type,flags,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START}
        wm.addView(view,p)
    }
    fun applyConfig(){ view?.applyConfig(OverlayBus.config) }
    fun newPattern(){view?.newPattern()}
    fun togglePause(){view?.togglePause()}
    private fun createChannel(){ if(Build.VERSION.SDK_INT>=26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("overlay","Ball Rival overlay",NotificationManager.IMPORTANCE_LOW)) }
    private fun notification():Notification{
        val pi=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val b=if(Build.VERSION.SDK_INT>=26) Notification.Builder(this,"overlay") else @Suppress("DEPRECATION") Notification.Builder(this)
        return b.setContentTitle("Ball Rival is running").setContentText("Tap for settings").setSmallIcon(android.R.drawable.ic_menu_view).setContentIntent(pi).setOngoing(true).build()
    }
    override fun onDestroy(){view?.let{runCatching{wm.removeView(it)}};view=null;OverlayBus.service=null;super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
}
