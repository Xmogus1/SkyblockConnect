package com.skyblockconnect.features.impl.achievements

object AchievementScanner {
    private val skillLevel = Regex("^SKILL LEVEL UP\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val skyblockLevel = Regex("Level (\\d+)[^\\d\\[]+\\[(\\d+)]", RegexOption.IGNORE_CASE)
    private val trophyFish = Regex("^TROPHY FISH!\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val rareDrop = Regex("^((?:CRAZY |VERY |INSANE )?RARE DROP!|PET DROP!)\\s*(.+)$", RegexOption.IGNORE_CASE)

    private val hoppityRabbit = Regex("^HOPPITY.?S HUNT\\s+You found\\s+(.+?)\\s+\\((\\w+)\\)!?$", RegexOption.IGNORE_CASE)

    private val chestHeader = Regex("^[A-Z ]*CHEST REWARDS$", RegexOption.IGNORE_CASE)

    private val chestNoise = Regex("(Essence\\s*x?[\\d,]*|\\bCoins\\b|^[=▬\\s-]*$)", RegexOption.IGNORE_CASE)

    private const val CHEST_MAX_MS = 10_000L
    private const val CHEST_MAX_ITEMS = 12

    private var chestOpen = false
    private var chestOpenedAt = 0L
    private var chestItems = 0

    fun scan(raw: String, inDungeon: Boolean, now: Long = System.currentTimeMillis()): Pair<AchievementKind, String>? {
        val text = raw.trim()

        if (text.isEmpty()) {
            chestOpen = false
            return null
        }

        if (text.startsWith("SBC ")) return null

        if (chestHeader.matches(text)) {
            chestOpen = true
            chestOpenedAt = now
            chestItems = 0
            return null
        }

        if (chestOpen) {
            val indented = raw.first().isWhitespace()
            val exhausted = now - chestOpenedAt > CHEST_MAX_MS || chestItems >= CHEST_MAX_ITEMS

            if (! indented || exhausted) {

                chestOpen = false
            }
            else {
                chestItems ++
                if (chestNoise.containsMatchIn(text)) return null
                return AchievementKind.DUNGEON_DROP to text
            }
        }

        skillLevel.find(text)?.let { return AchievementKind.SKILL_LEVEL to it.groupValues[1].trim() }
        skyblockLevel.find(text)?.let { return AchievementKind.SKYBLOCK_LEVEL to "SkyBlock Level ${it.groupValues[2]}" }
        trophyFish.find(text)?.let { return AchievementKind.TROPHY_FISH to it.groupValues[1].trim() }

        hoppityRabbit.find(text)?.let {

            val rarity = it.groupValues[2].trim().lowercase().replaceFirstChar(Char::uppercase)
            val name = it.groupValues[1].trim()
            return AchievementKind.HOPPITY_RABBIT to "$rarity Rabbit: $name"
        }

        rareDrop.find(text)?.let {

            val kind = if (inDungeon) AchievementKind.DUNGEON_DROP else AchievementKind.RARE_DROP
            return kind to it.groupValues[2].trim()
        }

        return null
    }

    fun reset() {
        chestOpen = false
        chestOpenedAt = 0L
        chestItems = 0
    }
}
