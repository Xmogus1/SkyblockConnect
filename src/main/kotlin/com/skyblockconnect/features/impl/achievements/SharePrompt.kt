package com.skyblockconnect.features.impl.achievements

import com.skyblockconnect.utils.ChatUtils
import com.skyblockconnect.websocket.SbcNet
import com.skyblockconnect.websocket.SbcSocket
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style

object SharePrompt {
    private const val EXPIRY_MS = 10 * 60 * 1000L
    private const val MAX_PENDING = 30

    private data class Pending(val kind: AchievementKind, val text: String, val at: Long)

    private val pending = LinkedHashMap<Int, Pending>()
    private var nextId = 1

    fun createOffer(kind: AchievementKind, text: String): Int {
        purge()

        val id = nextId ++
        pending[id] = Pending(kind, text, System.currentTimeMillis())
        while (pending.size > MAX_PENDING) pending.remove(pending.keys.first())
        return id
    }

    fun button(id: Int): Component = Component.literal(" §8[§a►§8]").withStyle(
        Style.EMPTY
            .withClickEvent(ClickEvent.RunCommand("/sbc share $id"))
            .withHoverEvent(HoverEvent.ShowText(Component.literal("§aShare this with everyone running SBC")))
    )

    fun share(id: Int) {
        purge()
        val entry = pending[id] ?: return ChatUtils.modMessage("§cThat share offer has expired.")

        if (! SbcSocket.connected) return ChatUtils.modMessage("§cNot connected to the relay - nothing was sent.")

        if (! SendCooldown.tryConsume()) {
            return ChatUtils.modMessage("§eOn cooldown - try again in ${SendCooldown.remainingSeconds()}s.")
        }

        if (! SbcNet.sendAchievement(entry.kind, entry.text)) {
            SendCooldown.reset()
            return ChatUtils.modMessage("§cCould not send that - the relay dropped the connection.")
        }

        pending.remove(id)
        ChatUtils.modMessage("§aShared.")
    }

    fun dismiss(id: Int) {
        pending.remove(id)
    }

    private fun purge() {
        val now = System.currentTimeMillis()
        pending.entries.removeAll { now - it.value.at > EXPIRY_MS }
    }
}
