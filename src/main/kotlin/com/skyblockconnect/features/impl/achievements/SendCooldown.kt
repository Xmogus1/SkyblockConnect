package com.skyblockconnect.features.impl.achievements

object SendCooldown {
    const val COOLDOWN_MS = 60_000L

    private var lastSendAt = 0L

    fun remainingMs(now: Long = System.currentTimeMillis()): Long =
        (COOLDOWN_MS - (now - lastSendAt)).coerceAtLeast(0L)

    val ready: Boolean get() = remainingMs() == 0L

    fun tryConsume(now: Long = System.currentTimeMillis()): Boolean {
        if (remainingMs(now) > 0L) return false
        lastSendAt = now
        return true
    }

    fun remainingSeconds(): Int = ((remainingMs() + 999L) / 1000L).toInt()

    fun reset() {
        lastSendAt = 0L
    }
}
