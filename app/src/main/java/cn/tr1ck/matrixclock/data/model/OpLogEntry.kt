package cn.tr1ck.matrixclock.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OpLogEntry(
    val timeMillis: Long,
    val action: String,
    val detail: String = "",
    val ip: String = ""
)
