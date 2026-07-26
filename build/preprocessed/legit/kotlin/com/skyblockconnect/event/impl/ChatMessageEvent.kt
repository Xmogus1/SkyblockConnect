package com.skyblockconnect.event.impl

import com.skyblockconnect.event.Event
import com.skyblockconnect.utils.ChatUtils.formattedText
import com.skyblockconnect.utils.ChatUtils.unformattedText
import net.minecraft.network.chat.Component

class ChatMessageEvent(val component: Component): Event(cancelable = true) {
    val formattedText by lazy { component.formattedText }
    val unformattedText by lazy { component.unformattedText }
}