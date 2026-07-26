package com.skyblockconnect.party

import com.skyblockconnect.event.EventBus
import com.skyblockconnect.event.impl.ChatMessageEvent
import com.skyblockconnect.event.impl.TickEvent
import com.skyblockconnect.features.impl.general.Connection
import com.skyblockconnect.hypixel.HypixelApi
import com.skyblockconnect.ui.notification.NotificationManager
import com.skyblockconnect.utils.ChatUtils
import com.skyblockconnect.websocket.SbcNet
import com.skyblockconnect.websocket.SbcSocket

object PartyFinder {

    val SKILLS: Map<String, Int> = linkedMapOf(
        "Combat" to 60, "Farming" to 60, "Mining" to 60, "Foraging" to 60,
        "Fishing" to 60, "Enchanting" to 60, "Alchemy" to 60, "Taming" to 60,
        "Carpentry" to 50, "Runecrafting" to 25, "Catacombs" to 50,
    )

    data class Listing(
        val leader: String,
        val note: String,
        val minLevel: Int,
        val reqs: String,
        val slots: Int,
        val members: List<String>,
        val ageMs: Long,
    )

    @Volatile
    var listings: List<Listing> = emptyList()
        private set

    var note = ""
    var minLevel = 0
    var slots = 6
        private set
    val skillReqs = LinkedHashMap<String, Int>()

    const val SLOTS_MIN = 2
    const val SLOTS_MAX = 10

    fun bumpSlots(delta: Int) {
        val next = (slots + delta).coerceIn(SLOTS_MIN, SLOTS_MAX)
        if (next == slots) return
        slots = next
        if (listed) pushListing()
    }

    var listed = false
        private set

    var inParty = false
        private set

    private val memberNames = linkedSetOf<String>()

    private var pendingJoinHost: String? = null
    private var pendingSince = 0L
    private var tickCounter = 0
    private var pollCounter = 0
    private const val REFRESH_TICKS = 240 * 20
    private const val POLL_TICKS = 60
    private const val JOIN_WINDOW_MS = 30_000L

    private val joinedParty = Regex("You have joined (\\w{1,16})'s party!", RegexOption.IGNORE_CASE)
    private val someoneJoined = Regex("(\\w{1,16}) joined the party", RegexOption.IGNORE_CASE)
    private val someoneLeft = Regex("(\\w{1,16}) has left the party", RegexOption.IGNORE_CASE)
    private val youLeft = Regex("You (?:left|have left) the party|You are not currently in a party|You have been (?:kicked|removed) from the party", RegexOption.IGNORE_CASE)
    private val disbanded = Regex("The party was disbanded", RegexOption.IGNORE_CASE)
    private val transferred = Regex("The party was transferred to (\\w{1,16})", RegexOption.IGNORE_CASE)
    private val promoted = Regex("(\\w{1,16}) has promoted (\\w{1,16}) to Party Leader", RegexOption.IGNORE_CASE)
    private val invited = Regex("(\\w{1,16}) has invited you to join (?:their|the) party", RegexOption.IGNORE_CASE)

    fun init() {
        EventBus.register<TickEvent.Start> {
            if (listed) {
                if (++ tickCounter >= REFRESH_TICKS) { tickCounter = 0; pushListing() }
                if (++ pollCounter >= POLL_TICKS) { pollCounter = 0; HypixelApi.queryParty() }
            }
            pendingJoinHost?.let { if (System.currentTimeMillis() - pendingSince > JOIN_WINDOW_MS) pendingJoinHost = null }
        }
        EventBus.register<ChatMessageEvent> { onChat(event.unformattedText.trim()) }
    }

    fun onPartyInfo(inParty: Boolean, canInvite: Boolean, memberCount: Int) {
        val leftOrDemoted = this.inParty && ! inParty || (this.inParty && inParty && ! canInvite)
        this.inParty = inParty
        if (! inParty) memberNames.clear()
        if (leftOrDemoted) { delistIfListed("party changed"); return }
        if (inParty && memberCount >= slots) delistIfListed("party is full")
    }

    private fun onChat(text: String) {
        joinedParty.find(text)?.let { inParty = true; memberNames.clear(); memberNames += it.groupValues[1] }
        someoneJoined.find(text)?.let { inParty = true; memberNames += it.groupValues[1]; checkFull() }
        someoneLeft.find(text)?.let { memberNames -= it.groupValues[1] }

        if (youLeft.containsMatchIn(text) || disbanded.containsMatchIn(text)) {
            inParty = false; memberNames.clear(); delistIfListed("you left the party")
        }
        transferred.find(text)?.let { if (! it.groupValues[1].equals(SbcNet.selfName, true)) delistIfListed("leadership transferred") }
        promoted.find(text)?.let { if (! it.groupValues[2].equals(SbcNet.selfName, true)) delistIfListed("someone else was promoted") }

        invited.find(text)?.let { m ->
            val host = m.groupValues[1]
            if (host.equals(pendingJoinHost, ignoreCase = true)) {
                pendingJoinHost = null
                ChatUtils.sendCommand("party accept $host")
            }
        }
    }

    private fun checkFull() {
        if (listed && currentMembers().size >= slots) delistIfListed("party is full")
    }

    private fun delistIfListed(why: String) {
        if (! listed) return
        unlist()
        NotificationManager.push("Party Finder", "Listing removed - $why.")
    }

    fun setListings(new: List<Listing>) { listings = new }

    fun reqsString(): String = skillReqs.entries.joinToString(", ") { "${it.key} ${it.value}" }

    fun canList(): Boolean = Connection.enabled && SbcSocket.connected

    fun toggleListed() = if (listed) unlist() else list()

    fun list() {
        if (! canList()) { NotificationManager.error("Party Finder", "Not connected to the relay yet."); return }
        pushListing()
        listed = true
        tickCounter = 0
        NotificationManager.push("Party Finder", "Your party is listed.")
    }

    fun unlist() {
        SbcNet.sendPartyUnlist()
        listed = false
    }

    private fun currentMembers(): List<String> =
        (listOf(SbcNet.selfName) + memberNames).distinctBy { it.lowercase() }

    private fun pushListing() =
        SbcNet.sendPartyList(note.trim(), minLevel.coerceIn(0, 1000), reqsString(), slots.coerceIn(SLOTS_MIN, SLOTS_MAX), currentMembers())

    fun join(listing: Listing) {
        val leader = listing.leader
        if (leader.equals(SbcNet.selfName, ignoreCase = true)) return
        if (! SbcSocket.connected) { NotificationManager.error("Party Finder", "Not connected to the relay."); return }

        pendingJoinHost = leader
        pendingSince = System.currentTimeMillis()
        SbcNet.sendPartyJoin(leader)
        ChatUtils.modMessage("§eAsked §f$leader §eto invite you - will auto-accept…")
    }

    fun onJoinDenied(reason: String) {
        pendingJoinHost = null
        NotificationManager.error("Party Finder", reason)
        ChatUtils.modMessage("§c$reason")
    }

    fun onJoinRequest(joiner: String) {
        if (! listed || joiner.equals(SbcNet.selfName, ignoreCase = true)) return
        ChatUtils.sendCommand("party invite $joiner")
        NotificationManager.push("Party Finder", "Inviting $joiner")
    }
}