package com.skyblockconnect.event.impl

import com.skyblockconnect.event.Event
import net.minecraft.world.entity.Entity

class CheckEntityRenderEvent(val entity: Entity): Event(cancelable = true)
