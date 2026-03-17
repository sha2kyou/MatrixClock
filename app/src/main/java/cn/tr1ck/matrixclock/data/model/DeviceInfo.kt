package cn.tr1ck.matrixclock.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val device: String,
    val androidVersion: String,
    val sdkInt: Int,
    val batteryLevel: Int,
    val batteryStatus: String,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val screenDensity: Float,
    val isCharging: Boolean
)
