package com.skyblockconnect.features.impl.events

import com.skyblockconnect.utils.ChatUtils.removeFormatting

object MiningEventParser {

    data class Parsed(val name: String, val secondsLeft: Int?)

    val KNOWN_EVENTS = listOf(
        "2x Powder",
        "Double Powder",
        "Better Together",
        "Goblin Raid",
        "Gone with the Wind",
        "Mithril Gourmand",
        "Raffle",
        "Great Odyssey",
        "Fallen Star Cult",
        "Mining Fiesta",
        "Fortunate Freezing",
    )

    private val knownPatterns = KNOWN_EVENTS.map { it to Regex("\\b${Regex.escape(it)}\\b", RegexOption.IGNORE_CASE) }

    private val withTimer = Regex(
        """\bEVENT\s+(.+?)\s+(?:ACTIVE|RUNNING|STARTING|STARTS)\s+IN\b.*?\bfor\s+(.+)$""",
        RegexOption.IGNORE_CASE,
    )

    private val withoutTimer = Regex(
        """\bEVENT\s+(.+?)\s+(?:ACTIVE|RUNNING|STARTING|STARTS)\s+IN\b.*$""",
        RegexOption.IGNORE_CASE,
    )

    private val durationPart = Regex("""(\d+)\s*([hms])""", RegexOption.IGNORE_CASE)

    fun parse(bar: String): Parsed? {
        val line = bar.removeFormatting().trim()
        if (line.isEmpty()) return null

        withTimer.find(line)?.let { m ->
            val name = m.groupValues[1].trim()
            if (name.isNotEmpty()) return Parsed(canonical(name), parseDuration(m.groupValues[2]))
        }

        withoutTimer.find(line)?.let { m ->
            val name = m.groupValues[1].trim()
            if (name.isNotEmpty()) return Parsed(canonical(name), parseDuration(line))
        }

        knownPatterns.firstOrNull { (_, pattern) -> pattern.containsMatchIn(line) }?.let { (name, _) ->
            return Parsed(name, parseDuration(line))
        }

        return null
    }

    private fun canonical(name: String): String =
        KNOWN_EVENTS.firstOrNull { it.equals(name, ignoreCase = true) } ?: name

    fun parseDuration(raw: String): Int? {
        var total = 0
        var matched = false
        durationPart.findAll(raw).forEach { m ->
            val n = m.groupValues[1].toIntOrNull() ?: return@forEach
            matched = true
            total += when (m.groupValues[2].lowercase()) {
                "h" -> n * 3600
                "m" -> n * 60
                else -> n
            }
        }
        return if (matched) total else null
    }

    fun formatDuration(seconds: Int): String = when {
        seconds >= 3600 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        seconds >= 60 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}