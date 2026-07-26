package com.skyblockconnect.ui.clickgui.components.impl

import com.skyblockconnect.config.Savable
import com.skyblockconnect.ui.clickgui.components.Setting
import com.skyblockconnect.ui.clickgui.components.Style
import com.skyblockconnect.ui.gui.Theme
import com.skyblockconnect.ui.utils.Animation
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.client.gui.GuiGraphicsExtractor

class ToggleSetting(name: String, value: Boolean = false): Setting<Boolean>(name, value), Savable {
    private val toggleAnim = Animation(200, if (value) 1f else 0f)
    private val hoverAnim = Animation(200, 0f)

    override fun draw(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        toggleAnim.update(if (value) 1f else 0f)
        hoverAnim.update(if (isHovered) 1f else 0f)

        Style.drawBackground(ctx, x, y, width, height)
        Style.drawHoverBar(ctx, x, y, height, hoverAnim.value)
        Style.drawNudgedText(ctx, name, x + 8f, y + 6f, hoverAnim.value)

        val sw = 26f
        val sh = 14f
        Theme.toggleSwitch(ctx, x + width - sw - 10f, y + (height / 2f) - (sh / 2f), sw, sh, toggleAnim.value)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            value = ! value
            Style.playClickSound(1f)
            return true
        }
        return false
    }

    override fun write() = JsonPrimitive(value)
    override fun read(element: JsonElement?) {
        value = element?.jsonPrimitive?.booleanOrNull ?: return
    }
}
