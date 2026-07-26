package com.skyblockconnect.websocket

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.skyblockconnect.SkyblockConnect.logger
import com.skyblockconnect.SkyblockConnect.mc
import com.skyblockconnect.features.AchievementFeature
import com.skyblockconnect.features.impl.achievements.AchievementKind
import com.skyblockconnect.features.impl.events.MiningEvents
import com.skyblockconnect.features.impl.events.VolcanoAnnounce
import com.skyblockconnect.features.impl.general.Connection
import com.skyblockconnect.party.PartyFinder
import com.skyblockconnect.utils.ChatUtils

object SbcNet {

    val selfName: String get() = runCatching { mc.user.name }.getOrDefault("?")

    fun sendMiningEvent(island: String, event: String, server: String?, endsInSeconds: Int?) = send("sbc_event") {
        addProperty("kind", "mining")
        addProperty("island", island)
        addProperty("event", event)
        server?.let { addProperty("server", it) }
        endsInSeconds?.let { addProperty("endsIn", it) }
    }.also { if (it) RecentFeed.record("Events", selfName, "§6§lMINING §r§e$event §7in §f$island") }

    fun sendVolcano(server: String?) = send("sbc_event") {
        addProperty("kind", "volcano")
        addProperty("island", "Crimson Isle")
        server?.let { addProperty("server", it) }
    }.also { if (it) RecentFeed.record("Events", selfName, "§6§lVOLCANO §r§clobby open §7on Crimson Isle") }

    fun sendAchievement(kind: AchievementKind, text: String) = send("sbc_achievement") {
        addProperty("kind", kind.wire)
        addProperty("text", text)
    }.also {
        if (it) RecentFeed.record(if (kind == AchievementKind.HOPPITY_RABBIT) "Rabbits" else "Achievements", selfName, "§f$selfName §7- §r$text")
    }

    fun sendPartyList(note: String, minLevel: Int, reqs: String, slots: Int, members: List<String>) = send("pf_list") {
        addProperty("note", note)
        addProperty("minLevel", minLevel)
        addProperty("reqs", reqs)
        addProperty("slots", slots)
        add("members", com.google.gson.JsonArray().apply { members.take(6).forEach(::add) })
    }

    fun sendPartyUnlist() = send("pf_unlist") {}

    fun sendPartyJoin(target: String) = send("pf_join") {
        addProperty("target", target)
    }

    private inline fun send(type: String, build: JsonObject.() -> Unit): Boolean {
        if (! SbcSocket.connected) return false
        val json = JsonObject()
        json.addProperty("type", type)
        json.build()
        return SbcSocket.send(json)
    }

    fun receive(raw: String) {
        val json = runCatching { JsonParser.parseString(raw) }.getOrNull()
            ?.takeIf(JsonElement::isJsonObject)?.asJsonObject ?: return

        when (json.str("type")) {
            "sbc_event" -> handleEvent(json)
            "sbc_events" -> json.getAsJsonArray("events")
                ?.mapNotNull { it as? JsonObject }
                ?.forEach { handleEvent(it, fromSnapshot = true) }

            "sbc_achievement" -> handleAchievement(json)
            "sbc_recent" -> handleRecent(json)
            "pf_listings" -> handlePartyListings(json)
            "pf_join" -> json.str("joiner")?.let { PartyFinder.onJoinRequest(it) }
            "pf_join_denied" -> PartyFinder.onJoinDenied(json.str("reason") ?: "You don't meet this party's requirements.")

            "message" -> json.str("text")?.let { ChatUtils.chat(it) }
            "ping", "cosmetics", "admin_result" -> Unit
            else -> logger.debug("SBC: ignoring unknown packet {}", raw.take(120))
        }
    }

    private fun handleEvent(json: JsonObject, fromSnapshot: Boolean = false) {
        val player = json.str("player")
        if (! Connection.showOwn.value && player != null && player.equals(selfName, true)) return

        when (json.str("kind")) {
            "mining" -> MiningEvents.onRemoteEvent(
                island = json.str("island") ?: return,
                event = json.str("event") ?: return,
                server = json.str("server"),
                endsIn = json.int("endsIn"),
                player = player,
                fromSnapshot = fromSnapshot,
            )

            "volcano" -> VolcanoAnnounce.onRemoteEvent(
                server = json.str("server"),
                player = player,
                fromSnapshot = fromSnapshot,
            )
        }
    }

    private fun handleAchievement(json: JsonObject) {
        val player = json.str("player") ?: return
        val text = json.str("text") ?: return
        val kind = AchievementKind.fromWire(json.str("kind")) ?: return
        if (! Connection.showOwn.value && player.equals(selfName, true)) return
        AchievementFeature.of(kind).onRemote(player, text)
    }

    private fun handleRecent(json: JsonObject) {
        val items = json.getAsJsonArray("items") ?: return
        RecentFeed.clear()
        items.mapNotNull { it as? JsonObject }.forEach { o ->
            val player = o.str("player")
            when (o.str("type")) {
                "sbc_event" -> when (o.str("kind")) {
                    "mining" -> MiningEvents.onRemoteEvent(
                        o.str("island") ?: return@forEach, o.str("event") ?: return@forEach,
                        o.str("server"), o.int("endsIn"), player, fromSnapshot = true, historyOnly = true,
                    )
                    "volcano" -> VolcanoAnnounce.onRemoteEvent(o.str("server"), player, fromSnapshot = true, historyOnly = true)
                }
                "sbc_achievement" -> {
                    val kind = AchievementKind.fromWire(o.str("kind")) ?: return@forEach
                    val text = o.str("text") ?: return@forEach
                    AchievementFeature.of(kind).onRemote(player ?: "?", text, historyOnly = true)
                }
            }
        }
    }

    private fun handlePartyListings(json: JsonObject) {
        val list = json.getAsJsonArray("listings")?.mapNotNull { it as? JsonObject }?.map { o ->
            PartyFinder.Listing(
                leader = o.str("leader") ?: return@map null,
                note = o.str("note").orEmpty(),
                minLevel = o.int("minLevel") ?: 0,
                reqs = o.str("reqs").orEmpty(),
                slots = o.int("slots") ?: 0,
                members = o.getAsJsonArray("members")?.mapNotNull { it?.asString?.takeIf(String::isNotBlank) }.orEmpty(),
                ageMs = o.get("ageMs")?.runCatching { asLong }?.getOrNull() ?: 0L,
            )
        }?.filterNotNull().orEmpty()
        PartyFinder.setListings(list)
    }

    private fun JsonObject.str(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf(String::isNotBlank)

    private fun JsonObject.int(key: String): Int? =
        get(key)?.takeIf { it.isJsonPrimitive }?.runCatching { asInt }?.getOrNull()
}