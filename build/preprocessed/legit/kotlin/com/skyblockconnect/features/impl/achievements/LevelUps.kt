package com.skyblockconnect.features.impl.achievements

import com.skyblockconnect.features.AchievementFeature

object LevelUps: AchievementFeature(
    "Skill and SkyBlock level ups.",
    listOf(AchievementKind.SKILL_LEVEL, AchievementKind.SKYBLOCK_LEVEL),

    autoAnnounceDefault = false,
    autoAnnounceDescription = "Send level ups straight away instead of asking first.",
    ultraRareOnly = false,
)