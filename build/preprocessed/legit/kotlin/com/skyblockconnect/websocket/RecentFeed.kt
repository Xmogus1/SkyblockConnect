package com.skyblockconnect.websocket

import com.skyblockconnect.utils.ChatUtils.removeFormatting

object RecentFeed {
    const val MAX = 50

    val categories = listOf("All", "Events", "Achievements", "Rabbits")

    data class Entry(val category: String, val player: String?, val text: String, val at: Long) {

        val plain: String = (player.orEmpty() + " " + text).removeFormatting().lowercase()
    }

    private val entries = ArrayDeque<Entry>()

    fun record(category: String, player: String?, text: String) = synchronized(entries) {
        entries.addFirst(Entry(category, player, text, System.currentTimeMillis()))
        while (entries.size > MAX) entries.removeLast()
    }

    fun view(category: String = "All", query: String = "", includeMine: Boolean = false): List<Entry> = synchronized(entries) {
        val q = query.trim().lowercase()
        val me = SbcNet.selfName
        entries.filter {
            (includeMine || ! it.player.equals(me, ignoreCase = true)) &&
                (category == "All" || it.category == category) &&
                (q.isEmpty() || q in it.plain)
        }
    }

    fun clear() = synchronized(entries) { entries.clear() }
}