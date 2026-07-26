package com.skyblockconnect.features.impl.achievements

enum class AchievementKind(val wire: String, val label: String, val color: String) {
    SKILL_LEVEL("skill_level", "Skill level up", "§b"),
    SKYBLOCK_LEVEL("skyblock_level", "SkyBlock level up", "§3"),
    RARE_DROP("rare_drop", "Rare drop", "§6"),
    TROPHY_FISH("trophy_fish", "Trophy fish", "§e"),
    DUNGEON_DROP("dungeon_drop", "Dungeon drop", "§d"),
    HOPPITY_RABBIT("hoppity_rabbit", "Hoppity's Hunt", "§d");

    companion object {
        fun fromWire(wire: String?) = entries.firstOrNull { it.wire == wire }
    }
}