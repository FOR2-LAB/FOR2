adb emu geo fix 77.594566 12.971599adb emu geo fix 77.594566 12.971599adb emu geo fix 77.594566 12.971599adb emu geo fix 77.594566 12.971599adb emu geo fix 77.594566 12.971599package com.for2.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var speedText: TextView
    private lateinit var etaText: TextView

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startLocationService()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        speedText = findViewById(R.id.speed_text)
        etaText = findViewById(R.id.eta_text)

        val startBtn: Button = findViewById(R.id.start_service_btn)
        startBtn.setOnClickListener {
            checkAndStart()
        }

        val stopBtn: Button = findViewById(R.id.stop_alarm_btn)
        stopBtn.setOnClickListener {
            // send intent to stop any alarm in service
            val i = Intent(this, LocationForegroundService::class.java)
            i.action = LocationForegroundService.ACTION_STOP_ALARM
            startService(i)
        }
    }

    private fun checkAndStart() {
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> startLocationService()
            else -> requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun startLocationService() {
        val i = Intent(this, LocationForegroundService::class.java)
        i.action = LocationForegroundService.ACTION_START
        startService(i)
    }

    // Called from service (later) to update UI
    fun updateSpeed(kmh: Double) {
        runOnUiThread { speedText.text = "Speed: ${String.format("%.1f", kmh)} km/h" }
    }
}
