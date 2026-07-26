package com.skyblockconnect.utils

import com.skyblockconnect.SkyblockConnect.mc
import com.skyblockconnect.utils.ChatUtils.removeFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam

object ScoreboardUtils {

    val title: String
        get() {
            val scoreboard = mc.level?.scoreboard ?: return ""
            val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return ""
            return objective.displayName.string.removeFormatting().trim()
        }

    val lines: List<String>
        get() {
            val scoreboard = mc.level?.scoreboard ?: return emptyList()
            val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()

            return scoreboard.listPlayerScores(objective)
                .sortedByDescending { it.value }
                .take(15)
                .map { score ->
                    val owner = score.ownerName().string
                    val team = scoreboard.getPlayersTeam(owner)
                    PlayerTeam.formatNameForTeam(team, Component.literal(owner)).string
                        .removeFormatting()

                        .replace("⏣", "")
                        .trim()
                }
        }

    fun valueOf(prefix: String): String? = lines
        .firstOrNull { it.startsWith(prefix, ignoreCase = true) }
        ?.substring(prefix.length)
        ?.trim()
        ?.takeIf(String::isNotEmpty)
}