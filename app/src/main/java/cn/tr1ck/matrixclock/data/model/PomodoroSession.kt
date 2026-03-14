package cn.tr1ck.matrixclock.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PomodoroSession(
    val configId: String,
    val configText: String,
    val startTimeMillis: Long,
    val plannedDurationSec: Int,
    val completed: Boolean,
    val actualDurationSec: Int
)
