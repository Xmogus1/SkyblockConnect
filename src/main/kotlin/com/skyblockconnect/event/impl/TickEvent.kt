package com.skyblockconnect.event.impl

import com.skyblockconnect.event.Event

abstract class TickEvent: Event(false) {
    object Start: TickEvent()
    object End: TickEvent()
    object Server: TickEvent()
}
