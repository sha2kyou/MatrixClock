package cn.tr1ck.matrixclock.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthRecord(
    val token: String,
    val ip: String,
    val deviceName: String = "",
    val deviceModel: String = "",
    val systemVersion: String = "",
    val batteryLevel: Int = -1,
    val createdAt: Long = System.currentTimeMillis()
)
