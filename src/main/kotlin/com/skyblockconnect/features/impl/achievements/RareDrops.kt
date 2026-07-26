package com.skyblockconnect.features.impl.achievements

import com.skyblockconnect.features.AchievementFeature

object RareDrops: AchievementFeature(
    "share rare drops",
    listOf(AchievementKind.RARE_DROP),

    autoAnnounceDefault = false,
    autoAnnounceDescription = "Auto announces ultra rare drops.",
)
