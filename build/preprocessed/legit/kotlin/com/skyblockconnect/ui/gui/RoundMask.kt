package com.skyblockconnect.ui.gui

import com.skyblockconnect.SkyblockConnect
import com.skyblockconnect.SkyblockConnect.mc
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.roundToInt

object RoundMask {
    private const val S = 96
    private const val HALF = S / 2

    private val id = Identifier.fromNamespaceAndPath(SkyblockConnect.MOD_ID, "round_mask")
    private var ready = false
    private var failed = false

    private fun ensure() {
        if (ready || failed) return
        try {
            val img = BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB)
            img.createGraphics().apply {
                setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                color = java.awt.Color(255, 255, 255, 255)
                fillOval(0, 0, S, S)
                dispose()
            }
            val png = ByteArrayOutputStream().also { ImageIO.write(img, "png", it) }.toByteArray()
            val native = NativeImage.read(png.inputStream())
            val tex = object : DynamicTexture({ "sbc-round-mask" }, native) {
                init { sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR) }
            }
            mc.textureManager.register(id, tex)
            ready = true
        }
        catch (t: Throwable) {
            failed = true
            SkyblockConnect.logger.warn("SBC: round-corner mask unavailable, falling back to square corners", t)
        }
    }

    fun corner(ctx: GuiGraphicsExtractor, x: Int, y: Int, r: Int, qx: Int, qy: Int, argb: Int) {
        if (r <= 0) return
        ensure()
        if (! ready) { ctx.fill(x, y, x + r, y + r, argb); return }
        ctx.blit(
            RenderPipelines.GUI_TEXTURED, id, x, y,
            (qx * HALF).toFloat(), (qy * HALF).toFloat(), r, r, HALF, HALF, S, S, argb,
        )
    }

    fun disc(ctx: GuiGraphicsExtractor, cx: Float, cy: Float, r: Float, argb: Int) {
        if (r <= 0f) return
        ensure()
        val d = (r * 2f).roundToInt()
        val x = (cx - r).roundToInt()
        val y = (cy - r).roundToInt()
        if (! ready) { ctx.fill(x, y, x + d, y + d, argb); return }
        ctx.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, d, d, S, S, S, S, argb)
    }
}