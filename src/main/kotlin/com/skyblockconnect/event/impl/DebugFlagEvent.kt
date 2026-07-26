package com.skyblockconnect.event.impl

import com.skyblockconnect.SkyblockConnect
import com.skyblockconnect.event.Event

sealed class DebugFlagEvent(val flag: String): Event(false) {
    class Add(flag: String): DebugFlagEvent(flag)
    class Remove(flag: String): DebugFlagEvent(flag)

    override fun cancel() {
        SkyblockConnect.debugFlags.remove(flag)
    }
}
