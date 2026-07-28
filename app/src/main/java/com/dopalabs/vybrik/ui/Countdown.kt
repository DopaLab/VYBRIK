package com.dopalabs.vybrik.ui

import androidx.compose.ui.graphics.Color
import kotlin.math.max

data class CountdownParts(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val expired: Boolean
) {
    fun compact(): String = when {
        expired -> "NOW"
        days > 0 -> "%02dd  %02dh  %02dm".format(days, hours, minutes)
        else -> "%02dh  %02dm  %02ds".format(hours, minutes, seconds)
    }
}

fun countdownParts(targetMillis: Long, nowMillis: Long): CountdownParts {
    val remaining = targetMillis - nowMillis
    val totalSeconds = max(0, remaining / 1000)
    return CountdownParts(
        days = totalSeconds / 86_400,
        hours = (totalSeconds % 86_400) / 3_600,
        minutes = (totalSeconds % 3_600) / 60,
        seconds = totalSeconds % 60,
        expired = remaining <= 0
    )
}

fun countdownColor(targetMillis: Long, nowMillis: Long): Color {
    val hours = (targetMillis - nowMillis) / 3_600_000.0
    return when {
        hours <= 1 -> Coral
        hours <= 24 -> Amber
        hours <= 168 -> Acid
        hours <= 720 -> Sky
        else -> Bone
    }
}
