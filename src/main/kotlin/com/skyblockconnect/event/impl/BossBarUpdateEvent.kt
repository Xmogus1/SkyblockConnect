package com.skyblockconnect.event.impl

import com.skyblockconnect.event.Event
import net.minecraft.network.chat.Component

class BossBarUpdateEvent(val name: Component, val progress: Float): Event(false)
