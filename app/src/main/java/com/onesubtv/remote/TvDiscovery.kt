package com.onesubtv.remote

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

data class TvDevice(val name: String, val host: String, val port: Int)

class TvDiscovery(context: Context, private val onChanged: (List<TvDevice>) -> Unit) {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val devices = linkedMapOf<String, TvDevice>()
    private var active = false

    private val listener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(type: String) { active = true }
        override fun onServiceFound(service: NsdServiceInfo) {
            if (service.serviceType.contains("_androidtvremote2")) {
                nsd.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(s: NsdServiceInfo, error: Int) = Unit
                    override fun onServiceResolved(s: NsdServiceInfo) {
                        val host = s.host?.hostAddress ?: return
                        devices[host] = TvDevice(s.serviceName, host, s.port)
                        onChanged(devices.values.toList())
                    }
                })
            }
        }
        override fun onServiceLost(service: NsdServiceInfo) {
            val key = devices.entries.firstOrNull { it.value.name == service.serviceName }?.key
            if (key != null) devices.remove(key)
            onChanged(devices.values.toList())
        }
        override fun onDiscoveryStopped(type: String) { active = false }
        override fun onStartDiscoveryFailed(type: String, error: Int) { active = false }
        override fun onStopDiscoveryFailed(type: String, error: Int) { active = false }
    }

    fun start() {
        if (!active) nsd.discoverServices("_androidtvremote2._tcp", NsdManager.PROTOCOL_DNS_SD, listener)
    }
    fun stop() {
        if (active) runCatching { nsd.stopServiceDiscovery(listener) }
        active = false
    }
}
