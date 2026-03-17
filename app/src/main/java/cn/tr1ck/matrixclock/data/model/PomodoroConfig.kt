package cn.tr1ck.matrixclock.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PomodoroConfig(
    val id: String,
    val text: String,
    val durationSec: Int,
    val colorHex: String,
    val isPrimary: Boolean = false,
    val countdownStyle: Int = 1,  // 1=two-line, 2=full-screen bar, 3=left text + right 32x32 matrix
    val cycleTotal: Int = 12      // daily cycle target (show x/cycleTotal, star when reached)
)
