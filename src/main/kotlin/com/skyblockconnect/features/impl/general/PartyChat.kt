package com.skyblockconnect.features.impl.general

import com.skyblockconnect.SkyblockConnect.mc
import com.skyblockconnect.event.impl.TickEvent
import com.skyblockconnect.features.Feature
import com.skyblockconnect.hypixel.HypixelApi
import com.skyblockconnect.party.PartyFinder
import com.skyblockconnect.utils.ChatUtils
import com.skyblockconnect.websocket.SbcNet
import com.skyblockconnect.websocket.SbcSocket
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket

object PartyChat: Feature("Party chat over the relay. While ON, your typed chat goes to your party instead of public chat.") {

    private var pollCounter = 0

    @Volatile
    var passthrough = false

    val deferred: Boolean
        get() = FabricLoader.getInstance().isModLoaded("tweaky")

    override fun init() {
        register<TickEvent.Start> {
            if (++ pollCounter >= 100) { pollCounter = 0; HypixelApi.queryParty() }
        }
    }

    override fun onEnable() {
        super.onEnable()
        HypixelApi.queryParty()
    }

    fun toggleFeature() {
        if (deferred) {
            ChatUtils.modMessage("§eParty Chat is handled by Tweaky while it's installed. Use Tweaky's §f/tweaky §eto manage it.")
            return
        }
        toggle()
        ChatUtils.modMessage(
            if (enabled) "§aParty Chat §2on §7- your chat now goes to your party"
            else "§cParty Chat §4off §7- back to normal chat"
        )
    }

    @JvmStatic
    fun interceptOutgoing(message: String): Boolean {
        if (deferred || passthrough || ! enabled || message.isBlank()) return false
        send(message)
        return true
    }

    @JvmStatic
    fun interceptOutgoingCommand(command: String): Boolean {
        if (deferred || passthrough || ! enabled) return false
        val body = partyChatCommandBody(command) ?: return false
        if (body.isBlank()) return false
        send(body)
        return true
    }

    private fun partyChatCommandBody(command: String): String? {
        val c = command.trimStart()
        return when {
            c.startsWith("pc ", true) -> c.substring(3)
            c.startsWith("pchat ", true) -> c.substring(6)
            c.startsWith("party chat ", true) -> c.substring(11)
            c.startsWith("p chat ", true) -> c.substring(7)
            else -> null
        }
    }

    fun send(messageRaw: String): Boolean {
        if (! enabled) { ChatUtils.modMessage("§cParty Chat is off. Type §f/wsc §cto turn it on."); return false }
        val message = sanitize(messageRaw)
        if (message.isEmpty()) return false
        if (! SbcSocket.connected) { ChatUtils.modMessage("§cNot connected to the relay."); return false }
        HypixelApi.queryParty()
        val recipients = recipients()
        if (recipients.isEmpty()) {
            val diag = if (HypixelApi.available) "" else " §8(Hypixel Mod API not installed - using chat tracking)"
            ChatUtils.modMessage("§cNo party members detected yet. Run §f/party list §conce, or talk in party chat.$diag")
            return false
        }
        if (! SbcNet.sendChat(message, recipients)) {
            ChatUtils.modMessage("§cCouldn't send, not connected to the relay.")
            return false
        }
        show(SbcNet.selfName, message)
        return true
    }

    fun onRemote(from: String, fromUuid: String?, textRaw: String) {
        if (deferred || ! enabled) return
        val text = sanitize(textRaw)
        if (text.isEmpty()) return
        show(from, text)
    }

    private fun recipients(): List<String> {
        val self = runCatching { mc.user.profileId }.getOrNull()
        val uuids = if (HypixelApi.available) HypixelApi.partyMembers.filter { it != self }.map { it.toString() } else emptyList()
        if (uuids.isNotEmpty()) return uuids
        return PartyFinder.partyRecipients()
    }

    private fun show(from: String, message: String) {
        if (message.startsWith("!")) {
            val line = Component.literal("§9Party §8> §b[MVP§c+§b] $from§f: §r").append(Component.literal(message))
            val conn = mc.player?.connection
            if (conn == null) { ChatUtils.chat(line); return }
            runCatching { conn.handleSystemChat(ClientboundSystemChatPacket(line, false)) }
                .onFailure { ChatUtils.chat(line) }
        }
        else {
            ChatUtils.chat(Component.literal("§5§lParty §r§f$from§7: §r").append(Component.literal(message)))
        }
    }

    private fun sanitize(text: String): String = text.replace("§", "").trim().take(256)
}
