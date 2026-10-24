package com.skyblockconnect.commands.impl

import com.skyblockconnect.commands.BaseCommand
import com.skyblockconnect.commands.CommandNodeBuilder
import com.skyblockconnect.features.impl.general.PartyChat
import com.mojang.brigadier.arguments.StringArgumentType

object WscCommand: BaseCommand("wsc") {
    override fun CommandNodeBuilder.build() {
        runs { PartyChat.toggleFeature() }
        argument("message", StringArgumentType.greedyString()) {
            runs { PartyChat.send(StringArgumentType.getString(it, "message")) }
        }
    }
}
