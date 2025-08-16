package com.lf.remoteeditor.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

object NetworkUtils {

    fun getDeviceIpAddress(context: Context): String? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return getLocalIpAddress() // Fallback for older versions
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return getLocalIpAddress()

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val linkProperties = connectivityManager.getLinkProperties(network)
            if (linkProperties != null) {
                for (linkAddress in linkProperties.linkAddresses) {
                    val address = linkAddress.address
                    if (address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        }
        return getLocalIpAddress() // Fallback for non-wifi or if not found
    }

    private fun getLocalIpAddress(): String? {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress) {
                        val hostAddress = address.hostAddress
                        // Check if it's IPv4 address
                        if (hostAddress != null && hostAddress.indexOf(':') < 0) {
                            return hostAddress
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun getNetworkInfo(context: Context): NetworkInfo {
        val ipAddress = getDeviceIpAddress(context) ?: "Unknown"
        val isWifi = isWifiConnected(context)
        val hostname = try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            "Android Device"
        }

        return NetworkInfo(
            ipAddress = ipAddress,
            isWifiConnected = isWifi,
            hostname = hostname
        )
    }

    data class NetworkInfo(
        val ipAddress: String,
        val isWifiConnected: Boolean,
        val hostname: String
    )
}