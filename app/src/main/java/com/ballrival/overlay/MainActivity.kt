package com.ballrival.overlay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity:AppCompatActivity(){
    private lateinit var minCount:SeekBar; private lateinit var maxCount:SeekBar; private lateinit var minSize:SeekBar; private lateinit var maxSize:SeekBar
    private lateinit var minStay:SeekBar; private lateinit var maxStay:SeekBar; private lateinit var opacity:SeekBar
    private lateinit var countLabel:TextView; private lateinit var sizeLabel:TextView; private lateinit var stayLabel:TextView; private lateinit var opacityLabel:TextView; private lateinit var status:TextView
    override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main)
        minCount=findViewById(R.id.minCount);maxCount=findViewById(R.id.maxCount);minSize=findViewById(R.id.minSize);maxSize=findViewById(R.id.maxSize);minStay=findViewById(R.id.minStay);maxStay=findViewById(R.id.maxStay);opacity=findViewById(R.id.opacity)
        countLabel=findViewById(R.id.countLabel);sizeLabel=findViewById(R.id.sizeLabel);stayLabel=findViewById(R.id.stayLabel);opacityLabel=findViewById(R.id.opacityLabel);status=findViewById(R.id.status)
        minCount.progress=2;maxCount.progress=6;minSize.progress=50;maxSize.progress=130;minStay.progress=2;maxStay.progress=6;opacity.progress=69
        val listener=object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,p:Int,f:Boolean){fixRanges(s);labels();if(f){readConfig();OverlayBus.service?.applyConfig()}};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}}
        listOf(minCount,maxCount,minSize,maxSize,minStay,maxStay,opacity).forEach{it.setOnSeekBarChangeListener(listener)};labels()
        findViewById<Button>(R.id.start).setOnClickListener{readConfig();startOverlay()};findViewById<Button>(R.id.pattern).setOnClickListener{OverlayBus.service?.newPattern()};findViewById<Button>(R.id.pause).setOnClickListener{OverlayBus.service?.togglePause()};findViewById<Button>(R.id.stop).setOnClickListener{stopService(Intent(this,OverlayService::class.java));status.text="Overlay stopped"}
    }
    private fun fixRanges(s:SeekBar?){if(s===minCount&&minCount.progress>maxCount.progress)maxCount.progress=minCount.progress;if(s===maxCount&&maxCount.progress<minCount.progress)minCount.progress=maxCount.progress;if(s===minSize&&minSize.progress>maxSize.progress)maxSize.progress=minSize.progress;if(s===maxSize&&maxSize.progress<minSize.progress)minSize.progress=maxSize.progress;if(s===minStay&&minStay.progress>maxStay.progress)maxStay.progress=minStay.progress;if(s===maxStay&&maxStay.progress<minStay.progress)minStay.progress=maxStay.progress}
    private fun readConfig(){OverlayBus.config=OverlayConfig(minCount.progress+1,maxCount.progress+1,minSize.progress+20,maxSize.progress+20,(minStay.progress+1)*1000L,(maxStay.progress+1)*1000L,opacity.progress+1)}
    private fun labels(){countLabel.text="Balls: ${minCount.progress+1} – ${maxCount.progress+1}";sizeLabel.text="Ball size: ${minSize.progress+20} – ${maxSize.progress+20} dp";stayLabel.text="Stay: ${minStay.progress+1} – ${maxStay.progress+1} sec";opacityLabel.text="Opacity: ${opacity.progress+1}%"}
    private fun startOverlay(){if(!Settings.canDrawOverlays(this)){status.text="Enable 'Display over other apps', then return";startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")));return};ContextCompat.startForegroundService(this,Intent(this,OverlayService::class.java));status.text="Overlay running — touches pass through"}
    override fun onResume(){super.onResume();if(Settings.canDrawOverlays(this))status.text="Overlay permission ready"}
}
