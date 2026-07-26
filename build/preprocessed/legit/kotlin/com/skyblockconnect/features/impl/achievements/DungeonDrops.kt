package com.skyblockconnect.features.impl.achievements

import com.skyblockconnect.features.AchievementFeature

object DungeonDrops: AchievementFeature(
    "Rare drops earned inside the Catacombs.",
    listOf(AchievementKind.DUNGEON_DROP),
    autoAnnounceDefault = false,
    autoAnnounceDescription = "Auto announces ultra rare drops.",
)