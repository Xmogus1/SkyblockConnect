package com.skyblockconnect.hypixel

import com.skyblockconnect.SkyblockConnect.logger
import com.skyblockconnect.SkyblockConnect.mc
import com.skyblockconnect.party.PartyFinder
import com.skyblockconnect.utils.ThreadUtils
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.handler.ClientboundPacketHandler
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket
import java.util.UUID

object HypixelApi {
    @Volatile
    var available = false
        private set

    @Volatile
    var partyMembers: Set<UUID> = emptySet()
        private set

    fun init() {
        try {
            HypixelModAPI.getInstance().registerHandler(ClientboundPartyInfoPacket::class.java, ClientboundPacketHandler { packet ->
                val myUuid = runCatching { mc.user.profileId }.getOrNull()
                val role = myUuid?.let { packet.memberMap[it]?.role }
                val canInvite = role == ClientboundPartyInfoPacket.PartyRole.LEADER || role == ClientboundPartyInfoPacket.PartyRole.MOD
                val members = runCatching { packet.memberMap.keys.toSet() }.getOrDefault(emptySet())
                partyMembers = if (packet.isInParty) members else emptySet()
                ThreadUtils.runOnMcThread { PartyFinder.onPartyInfo(packet.isInParty, canInvite, members.size) }
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
