package com.onesubtv.remote

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.connect).setOnClickListener {
            findViewById<TextView>(R.id.status).text = "Searching for Android TV / Google TV…"
            // Remote v2 transport: discovery, secure pairing, saved credentials and reconnect are next.
        }
    }
}
