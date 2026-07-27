package com.skyblockconnect.websocket

import com.skyblockconnect.config.PogObject

object BlockList {
    private val store = PogObject("blocked", mutableListOf<String>())

    val names: List<String> get() = store.get()

    fun isBlocked(name: String?): Boolean =
        name != null && store.get().any { it.equals(name, ignoreCase = true) }

    fun block(name: String): Boolean {
        val n = name.trim()
        if (n.isEmpty() || isBlocked(n)) return false
        store.get().add(n)
        store.save()
        return true
    }

    fun unblock(name: String): Boolean {
        if (! store.get().removeAll { it.equals(name.trim(), ignoreCase = true) }) return false
        store.save()
        return true
    }
}
