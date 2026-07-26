package com.skyblockconnect.features.impl.achievements

import com.skyblockconnect.features.AchievementFeature
import com.skyblockconnect.ui.clickgui.components.impl.MultiCheckboxSetting
import com.skyblockconnect.utils.ChatUtils.removeFormatting

object HoppityHunt: AchievementFeature(
    "New rabbits found during Hoppity's Hunt.",
    listOf(AchievementKind.HOPPITY_RABBIT),

    autoAnnounceDefault = true,
    autoAnnounceDescription = "Send matching rabbits straight away instead of asking first.",
    ultraRareOnly = false,
) {
    override val feedCategory = "Rabbits"

    private val defaults = linkedMapOf(
        "Common" to false, "Uncommon" to false, "Rare" to false,
        "Epic" to false, "Legendary" to false, "Mythic" to true, "Divine" to true,
    )

    val rarities by MultiCheckboxSetting("Rarities", LinkedHashMap(defaults))
        .withDescription("Which rabbit rarities to share and announce. Defaults to Mythic and Divine.")

    fun enabledRarities(): Set<String> = rarities.value.filterValues { it }.keys.mapTo(mutableSetOf()) { it.uppercase() }

    override fun shouldShare(text: String): Boolean {
        val rarity = rarityOf(text) ?: return false
        return rarity in enabledRarities()
    }

    override fun styleBody(body: String): String {
        val rarity = rarityOf(body) ?: return body
        return body.replaceFirst(
            Regex("^\\s*(\\w+) Rabbit:\\s*", RegexOption.IGNORE_CASE),
            "${rarityColor(rarity)}$1 Rabbit§7: §f",
        )
    }

    private val rarityPrefix = Regex("(\\w+) Rabbit:", RegexOption.IGNORE_CASE)

    fun rarityOf(text: String): String? =
        rarityPrefix.find(text.removeFormatting())?.groupValues?.get(1)?.uppercase()

    fun rarityColor(rarity: String): String = when (rarity.uppercase()) {
        "COMMON" -> "§f"
        "UNCOMMON" -> "§a"
        "RARE" -> "§9"
        "EPIC" -> "§5"
        "LEGENDARY" -> "§6"
        "MYTHIC" -> "§d"
        "DIVINE" -> "§b"
        else -> "§f"
    }
}