package com.onesubtv.remote

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var discovery: TvDiscovery
    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        findViewById<TextView>(R.id.status).text =
            if (granted) "Microphone ready — connect to a TV to use voice search"
            else "Microphone permission is needed for voice search"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val status = findViewById<TextView>(R.id.status)
        discovery = TvDiscovery(this) { devices ->
            runOnUiThread {
                status.text = if (devices.isEmpty()) "No TV found yet — keep both devices on the same Wi-Fi"
                else "Found: " + devices.joinToString { it.name }
            }
        }
        findViewById<Button>(R.id.connect).setOnClickListener {
            status.text = "Searching for Android TV / Google TV…"
            discovery.start()
        }
        findViewById<Button>(R.id.voice).setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                micPermission.launch(Manifest.permission.RECORD_AUDIO)
            else status.text = "Voice search ready — TV connection required"
        }
    }

    override fun onStop() {
        discovery.stop()
        super.onStop()
    }
}
