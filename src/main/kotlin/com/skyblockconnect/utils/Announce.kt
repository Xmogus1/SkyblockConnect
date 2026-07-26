package com.skyblockconnect.utils

import com.skyblockconnect.SkyblockConnect.PREFIX
import com.skyblockconnect.SkyblockConnect.mc
import com.skyblockconnect.utils.ChatUtils.addColor
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.sounds.SoundEvent

object Announce {
    fun chat(body: String, gg: String? = null, sound: SoundEvent? = null) {
        if (mc.player == null) return

        val line = Component.literal("$PREFIX ").append(Component.literal(body.addColor()))
        if (gg != null) line.append(ggButton(gg))
        ChatUtils.chat(line)

        sound?.let { ThreadUtils.runOnMcThread { mc.soundManager.play(SimpleSoundInstance.forUI(it, 1f, 0.6f)) } }
    }

    private fun ggButton(player: String): Component = Component.literal(" §8[§a§lgg§8]").withStyle(
        Style.EMPTY
            .withClickEvent(ClickEvent.RunCommand("/msg $player gg"))
            .withHoverEvent(HoverEvent.ShowText(Component.literal("§aClick to send §f$player §a\"gg\"")))
    )
}
