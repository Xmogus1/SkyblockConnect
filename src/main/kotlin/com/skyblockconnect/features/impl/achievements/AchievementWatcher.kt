package com.skyblockconnect.features.impl.achievements

import com.skyblockconnect.features.AchievementFeature
import com.skyblockconnect.utils.ChatUtils.removeFormatting
import com.skyblockconnect.utils.location.LocationUtils
import net.minecraft.network.chat.Component

object AchievementWatcher {

    private var lastText = ""
    private var lastAt = 0L

    @JvmStatic
    fun decorate(message: Component): Component {
        if (! LocationUtils.inSkyblock) return message

        val raw = message.string.removeFormatting()
        if (raw.isBlank()) {
            AchievementScanner.scan(raw, LocationUtils.inDungeon)
            return message
        }

        val now = System.currentTimeMillis()
        val (kind, body) = AchievementScanner.scan(raw, LocationUtils.inDungeon, now) ?: return message

        if (body == lastText && now - lastAt < 3_000L) return message
        lastText = body
        lastAt = now

        val feature = AchievementFeature.of(kind)
        val payload = "${kind.color}${kind.label}§7: §r${feature.styleBody(body)}"
        val offerId = feature.onLocal(kind, payload) ?: return message

        return Component.empty().append(message).append(SharePrompt.button(offerId))
    }
}
