package com.skyblockconnect.ui.gui

import com.skyblockconnect.features.impl.general.Interface
import com.skyblockconnect.utils.ColorUtils.lerp
import com.skyblockconnect.utils.ColorUtils.withAlpha
import com.skyblockconnect.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
import kotlin.math.roundToInt

object Theme {
    val accent: Color get() = Interface.accentColor.value
    val accentDim: Color get() = accent.withAlpha(70)

    val backdropTop = Color(10, 8, 18, 205)
    val backdropBottom = Color(4, 3, 8, 232)

    val windowTop = Color(27, 24, 41)
    val windowBottom = Color(19, 17, 30)
    val sidebar = Color(16, 14, 26)
    val header = Color(22, 20, 34)

    val bannerTop = Color(38, 28, 66)
    val bannerBottom = Color(23, 20, 40)

    val card = Color(31, 28, 47)
    val cardHover = Color(40, 37, 60)
    val cardInner = Color(24, 22, 38)

    val border = Color(58, 52, 88)
    val borderSoft = Color(44, 40, 66)

    val text = Color(238, 236, 248)
    val textMuted = Color(150, 145, 175)
    val textFaint = Color(104, 100, 128)

    val online = Color(88, 222, 128)
    val offline = Color(232, 92, 92)
    val pending = Color(240, 190, 80)

    fun disc(ctx: GuiGraphicsExtractor, cx: Float, cy: Float, r: Float, color: Color) =
        RoundMask.disc(ctx, cx, cy, r, color.rgb)

    fun roundRect(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, h: Float, r: Float, color: Color) {
        if (w <= 0f || h <= 0f) return
        val argb = color.rgb
        val xi = x.roundToInt(); val yi = y.roundToInt()
        val wi = w.roundToInt(); val hi = h.roundToInt()
        val ri = r.roundToInt().coerceIn(0, minOf(wi, hi) / 2)
        if (ri <= 0) { ctx.fill(xi, yi, xi + wi, yi + hi, argb); return }

        ctx.fill(xi, yi + ri, xi + wi, yi + hi - ri, argb)
        ctx.fill(xi + ri, yi, xi + wi - ri, yi + ri, argb)
        ctx.fill(xi + ri, yi + hi - ri, xi + wi - ri, yi + hi, argb)

        RoundMask.corner(ctx, xi, yi, ri, 0, 0, argb)
        RoundMask.corner(ctx, xi + wi - ri, yi, ri, 1, 0, argb)
        RoundMask.corner(ctx, xi, yi + hi - ri, ri, 0, 1, argb)
        RoundMask.corner(ctx, xi + wi - ri, yi + hi - ri, ri, 1, 1, argb)
    }

    fun roundRectRing(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, h: Float, r: Float, fill: Color, ring: Color, t: Float = 1f) {
        roundRect(ctx, x, y, w, h, r, ring)
        roundRect(ctx, x + t, y + t, w - t * 2f, h - t * 2f, (r - t).coerceAtLeast(0f), fill)
    }

    fun roundedTopBand(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, h: Float, r: Float, top: Color, bottom: Color) {
        val xi = x.roundToInt(); val yi = y.roundToInt()
        val wi = w.roundToInt(); val hi = h.roundToInt()
        val ri = r.roundToInt().coerceIn(0, minOf(wi, hi) / 2)
        if (ri <= 0) { Render2D.drawVerticalGradient(ctx, x, y, w, h, top, bottom); return }

        val topAtCorner = top.lerp(bottom, (ri.toFloat() / hi).coerceIn(0f, 1f))
        Render2D.drawVerticalGradient(ctx, xi.toFloat(), (yi + ri).toFloat(), wi.toFloat(), (hi - ri).toFloat(), topAtCorner, bottom)

        ctx.fill(xi + ri, yi, xi + wi - ri, yi + ri, top.rgb)
        RoundMask.corner(ctx, xi, yi, ri, 0, 0, top.rgb)
        RoundMask.corner(ctx, xi + wi - ri, yi, ri, 1, 0, top.rgb)
    }

    fun glowUnder(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, h: Float, color: Color) {
        Render2D.drawVerticalGradient(ctx, x, y, w, h, color.withAlpha(60), color.withAlpha(0))
    }

    fun capsule(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, h: Float, color: Color) =
        roundRect(ctx, x, y, w, h, minOf(w, h) / 2f, color)

    val toggleOff = Color(70, 66, 98)
    private val knobOff = Color(206, 203, 222)
    private val knobOn = Color(255, 255, 255)

    fun toggleSwitch(ctx: GuiGraphicsExtractor, x: Float, y: Float, w: Float, h: Float, progress: Float) {
        capsule(ctx, x, y, w, h, toggleOff.lerp(accent, progress))
        val knobR = h / 2f - 3f
        val cy = y + h / 2f
        val cxOff = x + h / 2f
        val cxOn = x + w - h / 2f
        val cx = cxOff + (cxOn - cxOff) * progress
        disc(ctx, cx, cy, knobR, knobOff.lerp(knobOn, progress))
    }

    fun statusPill(ctx: GuiGraphicsExtractor, x: Float, y: Float, dot: Color, label: String): Float {
        val w = with(Render2D) { label.width() } + 24f
        capsule(ctx, x, y, w, 16f, cardInner)
        disc(ctx, x + 9f, y + 8f, 2.5f, dot)
        Render2D.drawString(ctx, label, x + 16f, y + 4f, textMuted, 1, false)
        return w
    }
}