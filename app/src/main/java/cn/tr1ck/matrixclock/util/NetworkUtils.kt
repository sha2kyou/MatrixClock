package cn.tr1ck.matrixclock.util

import java.net.NetworkInterface

fun getLocalIpAddress(): String? {
    try {
        val en = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements()) {
            val intf = en.nextElement()
            val enumIpAddr = intf.inetAddresses
            while (enumIpAddr.hasMoreElements()) {
                val inetAddress = enumIpAddr.nextElement()
                if (!inetAddress.isLoopbackAddress && inetAddress is java.net.Inet4Address) {
                    val ip = inetAddress.hostAddress
                    if (ip != "0.0.0.0") return ip
                }
            }
        }
    } catch (e: Exception) {}
    return null
}
