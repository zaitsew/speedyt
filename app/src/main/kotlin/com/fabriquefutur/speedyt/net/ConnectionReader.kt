package com.fabriquefutur.speedyt.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.fabriquefutur.speedyt.analysis.ConnectionInfo
import com.fabriquefutur.speedyt.analysis.ConnectionKind

/**
 * Reads how the device is connected. Needs only ACCESS_NETWORK_STATE and
 * ACCESS_WIFI_STATE. It never reads the network name (SSID or BSSID) or an
 * address, and asks for no location permission.
 */
class ConnectionReader(context: Context) {
    private val appContext = context.applicationContext
    private val connectivity = appContext.getSystemService(ConnectivityManager::class.java)

    /** True when the system reports a network that can reach the internet. */
    fun hasInternet(): Boolean {
        val network = connectivity.activeNetwork ?: return false
        val caps = connectivity.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun read(): ConnectionInfo {
        val network = connectivity.activeNetwork ?: return ConnectionInfo(ConnectionKind.NONE)
        val caps = connectivity.getNetworkCapabilities(network) ?: return ConnectionInfo(ConnectionKind.NONE)
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionInfo(ConnectionKind.ETHERNET)
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> wifi(caps)
            else -> ConnectionInfo(ConnectionKind.OTHER)
        }
    }

    private fun wifi(caps: NetworkCapabilities): ConnectionInfo {
        // From API 31 the link details ride on the network's capabilities; only
        // the location-sensitive fields (SSID, BSSID) are redacted there.
        val fromCaps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) caps.transportInfo as? WifiInfo else null
        val info = fromCaps ?: legacyWifiInfo()
        if (info == null) return ConnectionInfo(ConnectionKind.WIFI)
        return ConnectionInfo(
            kind = ConnectionKind.WIFI,
            wifiFrequencyMhz = info.frequency.takeIf { it > 0 },
            signalDbm = info.rssi.takeIf { it in -126..0 },
            rxLinkMbps = rxLink(info),
            txLinkMbps = txLink(info),
        )
    }

    // Before Android 10 there is one rate, the transmit rate; receive is unknown.
    private fun rxLink(info: WifiInfo): Int? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) info.rxLinkSpeedMbps.takeIf { it > 0 } else null

    private fun txLink(info: WifiInfo): Int? {
        val tx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) info.txLinkSpeedMbps else info.linkSpeed
        return tx.takeIf { it > 0 }
    }

    @Suppress("DEPRECATION")
    private fun legacyWifiInfo(): WifiInfo? =
        appContext.getSystemService(WifiManager::class.java)?.connectionInfo
}
