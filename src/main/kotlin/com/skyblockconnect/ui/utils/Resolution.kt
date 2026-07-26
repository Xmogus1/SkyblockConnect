package com.skyblockconnect.ui.utils

import com.skyblockconnect.SkyblockConnect.mc
import com.skyblockconnect.utils.NumbersUtils.div
import com.skyblockconnect.utils.NumbersUtils.times
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.roundToInt

object Resolution {
    private const val REFERENCE_HEIGHT = 540f

    var scale = 1f
        private set

    var width = 960f
        private set

    var height = 540f
        private set

    fun refresh() {
        val window = Minecraft.getInstance().window
        val guiWidth = window.guiScaledWidth.toFloat()
        val guiHeight = window.guiScaledHeight.toFloat()

        scale = guiHeight / REFERENCE_HEIGHT

        height = REFERENCE_HEIGHT
        width = guiWidth / scale
    }

    fun push(ctx: GuiGraphicsExtractor) {
        ctx.pose().pushMatrix()
        ctx.pose().scale(scale, scale)
    }

    fun pop(ctx: GuiGraphicsExtractor) {
        ctx.pose().popMatrix()
    }

    fun getMouseX(vanillaX: Number) = (vanillaX / scale).toInt()
    fun getMouseY(vanillaY: Number) = (vanillaY / scale).toInt()

    fun getMouseX() = (mc.mouseHandler.xpos() / mc.window.screenWidth.toDouble() * width).toInt()
    fun getMouseY() = (mc.mouseHandler.ypos() / mc.window.screenHeight.toDouble() * height).toInt()

    fun toGuiScaled(value: Number) = (value * scale).roundToInt()

    fun scissor(ctx: GuiGraphicsExtractor, x1: Number, y1: Number, x2: Number, y2: Number) {
        val pose = ctx.pose()
        pose.pushMatrix()
        pose.identity()
        ctx.enableScissor(toGuiScaled(x1), toGuiScaled(y1), toGuiScaled(x2), toGuiScaled(y2))
        pose.popMatrix()
    }
}
