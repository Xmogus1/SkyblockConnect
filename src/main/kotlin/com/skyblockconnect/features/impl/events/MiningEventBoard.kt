package com.skyblockconnect.features.impl.events

object MiningEventBoard {
    private const val FALLBACK_LIFE_MS = 10 * 60 * 1000L

    data class Entry(
        val island: String,
        val server: String?,
        val event: String,

        val endsAt: Long?,
        val player: String?,
        val mine: Boolean,
        val seenAt: Long,
    ) {
        fun secondsLeft(now: Long): Int? = endsAt?.let { ((it - now) / 1000L).toInt() }

        fun expired(now: Long): Boolean =
            if (endsAt != null) now >= endsAt else now - seenAt > FALLBACK_LIFE_MS
    }

    private val entries = LinkedHashMap<String, Entry>()

    private fun key(island: String, server: String?) = "${server ?: "?"}|$island"

    fun put(island: String, server: String?, event: String, secondsLeft: Int?, player: String?, mine: Boolean) {
        val now = System.currentTimeMillis()
        entries[key(island, server)] = Entry(
            island = island,
            server = server,
            event = event,
            endsAt = secondsLeft?.let { now + it * 1000L },
            player = player,
            mine = mine,
            seenAt = now,
        )
    }

    fun live(now: Long = System.currentTimeMillis()): List<Entry> {
        entries.entries.removeAll { it.value.expired(now) }
        return entries.values.sortedWith(
            compareByDescending<Entry> { it.mine }.thenByDescending { it.secondsLeft(now) ?: Int.MIN_VALUE }
        )
    }

    fun own(now: Long = System.currentTimeMillis()): Entry? = live(now).firstOrNull { it.mine }

    fun baseline(now: Long = System.currentTimeMillis()): String? {
        val live = live(now)
        live.firstOrNull { it.mine }?.let { return it.event }
        return live.groupingBy { it.event }.eachCount().maxByOrNull { it.value }?.key
    }

    fun distinctEvents(now: Long = System.currentTimeMillis()): Int =
        live(now).map { it.event }.distinct().size

    fun clear() = entries.clear()
}
