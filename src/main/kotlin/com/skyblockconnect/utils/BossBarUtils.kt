package com.skyblockconnect.utils

import com.skyblockconnect.event.EventBus
import com.skyblockconnect.event.impl.BossBarUpdateEvent
import com.skyblockconnect.event.impl.WorldChangeEvent
import com.skyblockconnect.utils.ChatUtils.removeFormatting

object BossBarUtils {
    private const val STALE_MS = 30_000L
    private const val MAX_BARS = 8

    private val bars = LinkedHashMap<String, Long>()

    fun init() {
        EventBus.register<BossBarUpdateEvent> {
            val text = event.name.string.removeFormatting().trim()
            if (text.isEmpty()) return@register

            bars[text] = System.currentTimeMillis()
            while (bars.size > MAX_BARS) bars.remove(bars.keys.first())
        }

        EventBus.register<WorldChangeEvent> { bars.clear() }
    }

    fun lines(): List<String> {
        val now = System.currentTimeMillis()
        bars.entries.removeAll { now - it.value > STALE_MS }
        return bars.keys.toList()
    }

    fun find(predicate: (String) -> Boolean): String? = lines().lastOrNull(predicate)
}
