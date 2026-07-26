package com.skyblockconnect.event.impl

import com.skyblockconnect.event.Event
import net.minecraft.world.entity.Entity

class EntityUnloadEvent(val entity: Entity): Event(false)
