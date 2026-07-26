package com.skyblockconnect.ui.gui

import com.skyblockconnect.SkyblockConnect
import com.skyblockconnect.event.EventBus
import com.skyblockconnect.event.impl.KeyboardEvent
import com.skyblockconnect.features.impl.general.Interface
import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW

object MenuKeybind {
    fun init() = EventBus.register<KeyboardEvent.KeyPressed> {
        if (event.action != GLFW.GLFW_PRESS) return@register
        if (SkyblockConnect.mc.screen != null) return@register

        val bound = Interface.menuKey.value
        if (bound == InputConstants.UNKNOWN.value) return@register
        if (event.keyEvent.key != bound) return@register

        SkyblockConnect.screen = SbcScreen
    }
}
