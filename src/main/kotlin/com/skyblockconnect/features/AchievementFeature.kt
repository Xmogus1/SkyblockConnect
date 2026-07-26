package com.skyblockconnect.features

import com.skyblockconnect.features.impl.achievements.AchievementKind
import com.skyblockconnect.features.impl.achievements.DungeonDrops
import com.skyblockconnect.features.impl.achievements.HoppityHunt
import com.skyblockconnect.features.impl.achievements.LevelUps
import com.skyblockconnect.features.impl.achievements.RareDrops
import com.skyblockconnect.features.impl.achievements.SendCooldown
import com.skyblockconnect.features.impl.achievements.SharePrompt
import com.skyblockconnect.features.impl.achievements.TrophyFish
import com.skyblockconnect.features.impl.achievements.UltraRare
import com.skyblockconnect.features.impl.general.Connection
import com.skyblockconnect.ui.clickgui.components.impl.TextInputSetting
import com.skyblockconnect.ui.clickgui.components.impl.ToggleSetting
import com.skyblockconnect.utils.Announce
import com.skyblockconnect.utils.location.LocationUtils
import com.skyblockconnect.websocket.RecentFeed
import com.skyblockconnect.websocket.SbcNet
import net.minecraft.sounds.SoundEvents

abstract class AchievementFeature(
    description: String,
    val kinds: List<AchievementKind>,
    autoAnnounceDefault: Boolean = false,
    autoAnnounceDescription: String = "Auto announces ultra rare drops.",

    private val ultraRareOnly: Boolean = true,
): Feature(description, toggled = true) {

    val share by ToggleSetting("Share Mine", true)
        .withDescription("Allow your own of these to be shared. You still get a [Share] button before anything is sent.")

    val autoAnnounce by ToggleSetting("Auto Announce", autoAnnounceDefault)
        .withDescription(autoAnnounceDescription)
        .showIf { share.value }

    val autoExtra by TextInputSetting("Also Auto Announce", "")
        .withDescription("Comma-separated extra item names that skip the prompt, e.g. \"Ice Sprayer, Judgement Core\".")
        .showIf { share.value && autoAnnounce.value && ultraRareOnly }

    val receive by ToggleSetting("Show Others'", true)
        .withDescription("Print other players' announcements of this kind in your chat.")

    val playSound by ToggleSetting("Play Sound", false)
        .withDescription("Play a soft ping when one of these arrives.")

    open val feedCategory: String = "Achievements"

    protected open fun shouldShare(text: String): Boolean = true

    open fun styleBody(body: String): String = body

    fun onLocal(kind: AchievementKind, text: String): Int? {
        if (! enabled || ! share.value || ! shouldShare(text)) return null

        val auto = autoAnnounce.value && (! ultraRareOnly || UltraRare.matches(text, autoExtra.value))
        if (auto && SendCooldown.tryConsume()) {
            if (SbcNet.sendAchievement(kind, text)) return null

            SendCooldown.reset()
        }

        return SharePrompt.createOffer(kind, text)
    }

    fun onRemote(player: String, text: String, historyOnly: Boolean = false) {
        if (! enabled) return

        val body = "§f$player §7- §r$text"
        RecentFeed.record(feedCategory, player, body)
        if (historyOnly) return

        if (! receive.value) return
        if (Connection.quietWhenOffSkyblock.value && ! LocationUtils.inSkyblock) return

        Announce.chat(
            body,
            gg = player,
            sound = if (playSound.value) SoundEvents.NOTE_BLOCK_PLING.value() else null,
        )
    }

    companion object {

        fun of(kind: AchievementKind): AchievementFeature = when (kind) {
            AchievementKind.SKILL_LEVEL, AchievementKind.SKYBLOCK_LEVEL -> LevelUps
            AchievementKind.RARE_DROP -> RareDrops
            AchievementKind.TROPHY_FISH -> TrophyFish
            AchievementKind.DUNGEON_DROP -> DungeonDrops
            AchievementKind.HOPPITY_RABBIT -> HoppityHunt
        }
    }
}
