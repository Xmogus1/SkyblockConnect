package com.skyblockconnect.features.impl.achievements

import com.skyblockconnect.features.AchievementFeature

object RareDrops: AchievementFeature(
    "Rare, very rare, insane and pet drops outside of dungeons.",
    listOf(AchievementKind.RARE_DROP),

    autoAnnounceDefault = false,
    autoAnnounceDescription = "Auto announces ultra rare drops.",
)