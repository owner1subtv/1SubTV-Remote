package com.onesubtv.remote

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity() {
    private lateinit var discovery: TvDiscovery
    private var devices: List<TvDevice> = emptyList()
    private var remoteChannel: AndroidTvRemoteChannel? = null
    private var clientMaterial: AndroidTvCertFactory.Material? = null

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        status().text = if (granted) "Microphone ready" else "Microphone permission is needed for voice search"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.currentTime).text =
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        discovery = TvDiscovery(this) { found ->
            devices = found
            runOnUiThread {
                status().text = when (found.size) {
                    0 -> "Searching for your TV…"
                    1 -> "Found " + found.first().name + " — tap Connect"
                    else -> "Found " + found.size + " TVs — tap Connect"
                }
            }
        }

        findViewById<Button>(R.id.connect).setOnClickListener {
            if (devices.isEmpty()) {
                status().text = "Searching for Android TV / Google TV…"
                discovery.start()
            } else chooseAndPair(devices)
        }

        bindKey(R.id.power, RemoteKeyCode.POWER)
        bindKey(R.id.up, RemoteKeyCode.DPAD_UP)
        bindKey(R.id.down, RemoteKeyCode.DPAD_DOWN)
        bindKey(R.id.left, RemoteKeyCode.DPAD_LEFT)
        bindKey(R.id.right, RemoteKeyCode.DPAD_RIGHT)
        bindKey(R.id.ok, RemoteKeyCode.DPAD_CENTER)
        bindKey(R.id.home, RemoteKeyCode.HOME)
        bindKey(R.id.back, RemoteKeyCode.BACK)
        bindKey(R.id.volumeUp, RemoteKeyCode.VOLUME_UP)
        bindKey(R.id.volumeDown, RemoteKeyCode.VOLUME_DOWN)
        bindKey(R.id.mute, RemoteKeyCode.VOLUME_MUTE)
        bindKey(R.id.playPause, RemoteKeyCode.MEDIA_PLAY_PAUSE)

        findViewById<Button>(R.id.voice).setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                micPermission.launch(Manifest.permission.RECORD_AUDIO)
            else status().text = "Voice search is being finalized"
        }

        status().text = "Searching for your TV…"
        discovery.start()
    }

    private fun chooseAndPair(found: List<TvDevice>) {
        if (found.size == 1) pair(found.first()) else {
            val names = found.map { it.name }.toTypedArray()
            AlertDialog.Builder(this).setTitle("Choose your TV")
                .setItems(names) { _, which -> pair(found[which]) }.show()
        }
    }

    private fun pair(device: TvDevice) {
        status().text = "Connecting to " + device.name + "…"
        lifecycleScope.launch {
            val material = clientMaterial ?: withContext(Dispatchers.Default) {
                AndroidTvCertFactory.generate()
            }.also { clientMaterial = it }

            val result = AndroidTvPairingChannel(
                host = device.host,
                pairingPort = 6467,
                clientMaterial = material,
                clientName = "1SubTV Remote"
            ).pair { promptForPairingCode(device.name) }

            result.fold(
                onSuccess = { paired ->
                    runCatching {
                        withContext(Dispatchers.IO) {
                            remoteChannel?.close()
                            remoteChannel = AndroidTvRemoteChannel(
                                host = device.host,
                                port = device.port,
                                clientMaterial = material,
                                expectedFingerprint = paired.serverCertSha256
                            ).also { channel ->
                                channel.connect { error ->
                                    runOnUiThread {
                                        status().text = if (error == null) "Disconnected" else "TV disconnected"
                                    }
                                }
                            }
                        }
                    }.onSuccess {
                        status().text = "Connected to " + device.name
                    }.onFailure {
                        status().text = "Paired, but remote connection failed"
                    }
                },
                onFailure = { status().text = "Pairing failed: " + (it.message ?: "try again") }
            )
        }
    }

    private suspend fun promptForPairingCode(tvName: String): String? =
        suspendCancellableCoroutine { continuation ->
            runOnUiThread {
                val input = EditText(this).apply {
                    hint = "6-character code"
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                }
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Pair with " + tvName)
                    .setMessage("Enter the code shown on your TV.")
                    .setView(input)
                    .setPositiveButton("Pair", null)
                    .setNegativeButton("Cancel") { _, _ ->
                        if (continuation.isActive) continuation.resume(null)
                    }.create()
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val code = input.text.toString().trim()
                        if (code.length == 6) {
                            dialog.dismiss()
                            if (continuation.isActive) continuation.resume(code)
                        } else input.error = "Enter the 6-character code"
                    }
                }
                dialog.setOnCancelListener {
                    if (continuation.isActive) continuation.resume(null)
                }
                dialog.show()
            }
        }

    private fun bindKey(id: Int, key: RemoteKeyCode) {
        findViewById<Button>(id).setOnClickListener {
            val channel = remoteChannel
            if (channel == null) {
                status().text = "Connect to your TV first"
                return@setOnClickListener
            }
            lifecycleScope.launch {
                runCatching { channel.send(RemoteMessage.KeyInject(key, RemoteDirection.SHORT)) }
                    .onFailure { status().text = "Remote command failed — reconnecting may help" }
            }
        }
    }

    private fun status() = findViewById<TextView>(R.id.status)

    override fun onStop() {
        discovery.stop()
        super.onStop()
    }

    override fun onDestroy() {
        remoteChannel?.close()
        super.onDestroy()
    }
}
