package com.skyblockconnect.ui.clickgui.components

import com.skyblockconnect.SkyblockConnect.mc
import com.skyblockconnect.features.impl.general.Interface
import com.skyblockconnect.ui.gui.Theme
import com.skyblockconnect.utils.ColorUtils.withAlpha
import com.skyblockconnect.utils.MathUtils
import com.skyblockconnect.utils.NumbersUtils.div
import com.skyblockconnect.utils.NumbersUtils.minus
import com.skyblockconnect.utils.NumbersUtils.plus
import com.skyblockconnect.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents
import java.awt.Color

object Style {
    val accentColor get() = Interface.accentColor.value
    val accentColorTrans get() = accentColor.withAlpha(120)

    val bg: Color get() = Theme.cardInner

    private val trackColor = Color(52, 47, 76)

    fun drawBackground(ctx: GuiGraphicsExtractor, x: Number, y: Number, w: Number, h: Number) {
        Render2D.drawRect(ctx, x, y, w, h, bg)
    }

    fun drawHoverBar(ctx: GuiGraphicsExtractor, x: Number, y: Number, height: Number, anim: Float) {
        if (anim <= 0.01f) return
        val barH = (height - 6f) * anim
        val barY = y + (height / 2f) - (barH / 2f)
        Render2D.drawRect(ctx, x, barY, 2f, barH, accentColor.withAlpha((220 * anim).toInt()))
    }

    fun drawNudgedText(ctx: GuiGraphicsExtractor, text: String, x: Float, y: Float, anim: Float, color: Color = Theme.text) {
        Render2D.drawString(ctx, text, x + 2f * anim, y, color, 1, true)
    }

    fun drawSlider(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, progress: Float, hoverAnim: Float, color: Color) {
        val h = 3f
        Render2D.drawRect(ctx, x, y, w, h, trackColor)
        val barColor = MathUtils.lerpColor(Color(color.red, color.green, color.blue, 200), color, hoverAnim)
        Render2D.drawRect(ctx, x, y, w * progress, h, barColor)
        val kSize = 5f
        Render2D.drawRect(ctx, x + (w * progress) - (kSize / 2f), y + (h / 2f) - (kSize / 2f), kSize, kSize, Theme.text)
    }

    fun playClickSound(pitch: Float) {
        if (! Interface.playClickSound.value) return
        mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch))
    }
}
