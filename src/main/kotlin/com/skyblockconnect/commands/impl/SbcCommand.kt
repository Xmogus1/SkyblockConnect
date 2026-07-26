package com.skyblockconnect.commands.impl

import com.skyblockconnect.SkyblockConnect.screen
import com.skyblockconnect.commands.BaseCommand
import com.skyblockconnect.commands.CommandNodeBuilder
import com.skyblockconnect.features.impl.achievements.SharePrompt
import com.skyblockconnect.features.impl.general.Connection
import com.skyblockconnect.hypixel.HypixelApi
import com.skyblockconnect.ui.gui.PartyFinderScreen
import com.skyblockconnect.ui.gui.RecentScreen
import com.skyblockconnect.ui.gui.SbcScreen
import com.skyblockconnect.ui.hud.HudEditorScreen
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.skyblockconnect.utils.ChatUtils
import com.skyblockconnect.websocket.SbcSocket

object SbcCommand: BaseCommand("sbc", mutableSetOf("skyblockconnect")) {
    private val help = mapOf(
        "/sbc" to "toggle stuff",
        "/sbc status" to "Relay connection status",
        "/sbc reconnect" to "Reconnect to the relay",
        "/sbc recent" to "share history",
        "/sbc pf" to "find or list parties",
        "/sbc hud" to "Move the mining event HUD",
    )

    override fun CommandNodeBuilder.build() {
        runs { screen = SbcScreen }

        literal("help") {
            runs {
                val out = StringBuilder("§6§lSkyblock Connect§r\n")
                help.forEach { (cmd, desc) -> out.append("§e$cmd §7- $desc\n") }
                ChatUtils.chat(out.toString().trim())
            }
        }

        literal("hud") {
            runs { screen = HudEditorScreen }
        }

        literal("recent") {
            runs { screen = RecentScreen }
        }

        literal("pf") {
            runs { openPartyFinder() }
        }
        literal("partyfinder") {
            runs { openPartyFinder() }
        }

        literal("status") {
            runs { ChatUtils.modMessage(Connection.status()) }
        }

        literal("reconnect") {
            runs {
                if (! Connection.enabled) ChatUtils.modMessage("§cConnection is turned off.")
                else {
                    SbcSocket.reconnect()
                    ChatUtils.modMessage("§eReconnecting…")
                }
            }
        }

        literal("share") {
            argument("id", IntegerArgumentType.integer(1)) {
                runs { SharePrompt.share(IntegerArgumentType.getInteger(it, "id")) }
            }
        }

        literal("dismiss") {
            argument("id", IntegerArgumentType.integer(1)) {
                runs { SharePrompt.dismiss(IntegerArgumentType.getInteger(it, "id")) }
            }
        }
    }

    private fun openPartyFinder() {
        HypixelApi.queryParty()
        screen = PartyFinderScreen
    }
}
