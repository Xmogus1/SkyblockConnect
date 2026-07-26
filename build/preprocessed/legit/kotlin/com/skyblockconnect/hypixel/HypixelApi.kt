package com.skyblockconnect.hypixel

import com.skyblockconnect.SkyblockConnect.logger
import com.skyblockconnect.SkyblockConnect.mc
import com.skyblockconnect.party.PartyFinder
import com.skyblockconnect.utils.ThreadUtils
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.handler.ClientboundPacketHandler
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket

object HypixelApi {
    @Volatile
    var available = false
        private set

    fun init() {
        try {
            HypixelModAPI.getInstance().registerHandler(ClientboundPartyInfoPacket::class.java, ClientboundPacketHandler { packet ->
                val myUuid = runCatching { mc.user.profileId }.getOrNull()
                val role = myUuid?.let { packet.memberMap[it]?.role }
                val canInvite = role == ClientboundPartyInfoPacket.PartyRole.LEADER || role == ClientboundPartyInfoPacket.PartyRole.MOD
                val count = runCatching { packet.memberMap.size }.getOrDefault(0)
                ThreadUtils.runOnMcThread { PartyFinder.onPartyInfo(packet.isInParty, canInvite, count) }
            })
            available = true
            logger.info("SBC: hooked the Hypixel Mod API for party info")
        }
        catch (t: Throwable) {
            available = false
            logger.info("SBC: Hypixel Mod API unavailable - party detection falls back to chat")
        }
    }

    fun queryParty() {
        if (! available) return
        runCatching { HypixelModAPI.getInstance().sendPacket(ServerboundPartyInfoPacket()) }
    }
}