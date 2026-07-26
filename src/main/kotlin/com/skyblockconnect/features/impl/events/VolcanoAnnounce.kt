package com.skyblockconnect.features.impl.events

import com.skyblockconnect.event.impl.ChatMessageEvent
import com.skyblockconnect.features.Feature
import com.skyblockconnect.features.impl.general.Connection
import com.skyblockconnect.ui.clickgui.components.impl.ButtonSetting
import com.skyblockconnect.ui.clickgui.components.impl.ToggleSetting
import com.skyblockconnect.ui.notification.NotificationManager
import com.skyblockconnect.utils.Announce
import com.skyblockconnect.utils.location.LocationUtils
import com.skyblockconnect.utils.location.WorldType
import com.skyblockconnect.websocket.RecentFeed
import com.skyblockconnect.websocket.SbcNet
import net.minecraft.sounds.SoundEvents

object VolcanoAnnounce: Feature("share volcano lobbies", toggled = true) {

    val share by ToggleSetting("Auto-Announce My Lobby", false)
        .withDescription("Automatically tell everyone when a volcano erupts in your lobby.")

    val receive by ToggleSetting("Show Others'", true)
        .withDescription("Print volcano lobbies other players announce.")

    private val playSound by ToggleSetting("Play Sound", true)
        .withDescription("Play a ping when a volcano lobby arrives.")

    private val announceNow by ButtonSetting("Announce This Lobby") {
        announce(manual = true)
    }.withDescription("Send your current lobby id once, without turning auto-announce on.")

    private val volcanoLine = Regex("CATACLYSMIC VOLCANO", RegexOption.IGNORE_CASE)

    private var lastAnnouncedServer: String? = null

    override fun init() {
        register<ChatMessageEvent> {
            if (! share.value || ! LocationUtils.inSkyblock) return@register
            if (LocationUtils.world != WorldType.CrimsonIsle) return@register
            if (! volcanoLine.containsMatchIn(event.unformattedText)) return@register

            announce(manual = false)
        }
    }

    private fun announce(manual: Boolean) {
        val server = LocationUtils.serverId
        if (! manual && server != null && server == lastAnnouncedServer) return

        if (manual) {
            if (LocationUtils.world != WorldType.CrimsonIsle) {
                NotificationManager.error("SBC", "You are not in the Crimson Isle.")
                return
            }
            if (! SbcNet.sendVolcano(server)) {
                NotificationManager.error("SBC", "Not connected to the relay.")
                return
            }
            NotificationManager.push("SBC", "Announced ${server ?: "your lobby"}")
        }
        else SbcNet.sendVolcano(server)

        lastAnnouncedServer = server
    }

    fun onRemoteEvent(server: String?, player: String?, fromSnapshot: Boolean, historyOnly: Boolean = false) {
        if (! enabled) return

        val where = server?.let { " §8(§f$it§8)" }.orEmpty()
        val who = if (fromSnapshot || player == null) "" else " §8· §7from §f$player"
        val body = "§c§lVOLCANO §r§eCataclysmic Volcano §7in the Crimson Isle$where$who"
        RecentFeed.record("Events", player, body)
        if (historyOnly) return

        if (! receive.value) return
        if (Connection.quietWhenOffSkyblock.value && ! LocationUtils.inSkyblock) return

        Announce.chat(
            body,
            gg = player.takeUnless { fromSnapshot },
            sound = if (playSound.value) SoundEvents.NOTE_BLOCK_PLING.value() else null,
        )
    }
}
