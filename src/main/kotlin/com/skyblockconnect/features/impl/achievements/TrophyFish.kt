package com.skyblockconnect.features.impl.achievements

import com.skyblockconnect.features.AchievementFeature

object TrophyFish: AchievementFeature(
    "share trophy fish",
    listOf(AchievementKind.TROPHY_FISH),
    autoAnnounceDefault = false,
    autoAnnounceDescription = "Send trophy fish straight away instead of asking first.",
    ultraRareOnly = false,
)
