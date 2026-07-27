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
import com.mojang.brigadier.arguments.StringArgumentType
import com.skyblockconnect.utils.ChatUtils
import com.skyblockconnect.websocket.BlockList
import com.skyblockconnect.websocket.SbcSocket

object SbcCommand: BaseCommand("sbc", mutableSetOf("skyblockconnect")) {
    private val help = mapOf(
        "/sbc" to "toggle stuff",
        "/sbc status" to "Relay connection status",
        "/sbc reconnect" to "Reconnect to the relay",
        "/sbc recent" to "share history",
        "/sbc pf" to "find or list parties",
        "/sbc hud" to "Move the mining event HUD",
        "/sbc block <player>" to "hide someone's shares",
        "/sbc unblock <player>" to "unhide someone",
        "/sbc blocks" to "list blocked players",
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

        literal("block") {
            argument("player", StringArgumentType.word()) {
                runs {
                    val p = StringArgumentType.getString(it, "player")
                    if (BlockList.block(p)) ChatUtils.modMessage("§7blocked §f$p§7, you won't see their shares")
                    else ChatUtils.modMessage("§f$p §7is already blocked")
                }
            }
        }

        literal("unblock") {
            argument("player", StringArgumentType.word()) {
                runs {
                    val p = StringArgumentType.getString(it, "player")
                    if (BlockList.unblock(p)) ChatUtils.modMessage("§7unblocked §f$p")
                    else ChatUtils.modMessage("§f$p §7is not blocked")
                }
            }
        }

        literal("blocks") {
            runs {
                val list = BlockList.names
                if (list.isEmpty()) ChatUtils.modMessage("§7you haven't blocked anyone")
                else ChatUtils.modMessage("§7blocked (§f${list.size}§7): §f${list.joinToString("§7, §f")}")
            }
        }
    }

    private fun openPartyFinder() {
        HypixelApi.queryParty()
        screen = PartyFinderScreen
    }
}
