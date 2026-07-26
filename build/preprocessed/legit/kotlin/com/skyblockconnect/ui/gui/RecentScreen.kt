package com.skyblockconnect.ui.gui

import com.skyblockconnect.SkyblockConnect
import com.skyblockconnect.ui.clickgui.components.Style
import com.skyblockconnect.ui.utils.Animation
import com.skyblockconnect.ui.utils.Resolution
import com.skyblockconnect.ui.utils.TextInputHandler
import com.skyblockconnect.utils.ColorUtils.withAlpha
import com.skyblockconnect.utils.render.Render2D
import com.skyblockconnect.utils.render.Render2D.width
import com.skyblockconnect.websocket.RecentFeed
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

object RecentScreen: Screen(Component.literal("SBC Recent")) {

    private const val windowWidth = 540f
    private const val windowHeight = 372f
    private const val bannerHeight = 46f
    private const val tabBarHeight = 32f
    private const val rowHeight = 30f
    private const val pad = 16f

    private val darkText = java.awt.Color(20, 18, 12)

    private var winX = 0f
    private var winY = 0f
    private var listTop = 0f
    private var listBottom = 0f

    private var selectedCategory = "All"
    private var searchQuery = ""
    private var showMine = false
    private var scrollTarget = 0f
    private var maxScroll = 0f
    private val scrollAnim = Animation(220L)
    private val openAnim = Animation(240L)
    private var needsOpenAnim = true

    private val search = TextInputHandler({ searchQuery }, { searchQuery = it })

    @JvmField
    var modMenuParent: Screen? = null

    private fun tabRects(): List<Pair<String, ClosedFloatingPointRange<Float>>> {
        val out = mutableListOf<Pair<String, ClosedFloatingPointRange<Float>>>()
        var tx = winX + pad
        RecentFeed.categories.forEach { cat ->
            val w = "§l$cat".width().toFloat() + 22f
            out += cat to (tx..(tx + w))
            tx += w + 6f
        }
        return out
    }

    private fun relativeTime(now: Long, at: Long): String {
        val s = ((now - at) / 1000L).toInt()
        return when {
            s < 5 -> "now"
            s < 60 -> "${s}s"
            s < 3600 -> "${s / 60}m"
            else -> "${s / 3600}h"
        }
    }

    override fun extractRenderState(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        Resolution.refresh()
        Resolution.push(ctx)
        val mX = Resolution.getMouseX(mouseX.toDouble())
        val mY = Resolution.getMouseY(mouseY.toDouble())

        if (needsOpenAnim) { openAnim.set(0f); needsOpenAnim = false }
        openAnim.update(1f)
        val open = openAnim.value

        ctx.fillGradient(
            0, 0, Resolution.width.toInt(), Resolution.height.toInt(),
            Theme.backdropTop.withAlpha((Theme.backdropTop.alpha * open).toInt()).rgb,
            Theme.backdropBottom.withAlpha((Theme.backdropBottom.alpha * open).toInt()).rgb,
        )

        winX = ((Resolution.width - windowWidth) / 2f).coerceAtLeast(0f)
        winY = ((Resolution.height - windowHeight) / 2f).coerceAtLeast(0f)

        val pose = ctx.pose()
        val popping = open < 0.999f
        if (popping) {
            val scale = 0.96f + 0.04f * open
            val px = winX + windowWidth / 2f
            val py = winY + windowHeight / 2f
            pose.pushMatrix(); pose.translate(px, py); pose.scale(scale); pose.translate(-px, -py)
        }

        Theme.roundRectRing(ctx, winX, winY, windowWidth, windowHeight, 7f, Theme.windowBottom, Theme.border)
        drawBanner(ctx, mX, mY)
        drawTabs(ctx, mX, mY)
        drawList(ctx, mX, mY)

        if (popping) pose.popMatrix()
        Resolution.pop(ctx)
    }

    private fun drawBanner(ctx: GuiGraphicsExtractor, mX: Int, mY: Int) {
        Theme.roundedTopBand(ctx, winX + 1f, winY + 1f, windowWidth - 2f, bannerHeight, 6f, Theme.bannerTop, Theme.bannerBottom)
        Render2D.drawRect(ctx, winX + 1f, winY + bannerHeight, windowWidth - 2f, 1f, Theme.accent.withAlpha(210))
        Theme.disc(ctx, winX + pad + 6f, winY + 16f, 5f, Theme.accent)
        Render2D.drawString(ctx, "§lRECENT", winX + pad + 16f, winY + 11f, Theme.text)
        Render2D.drawString(ctx, "share history", winX + pad + 16f, winY + 25f, Theme.textFaint, 0.85f, false)

        val bw = 150f
        val bh = 18f
        val bx = winX + windowWidth - pad - bw
        val by = winY + (bannerHeight - bh) / 2f
        Theme.roundRect(ctx, bx, by, bw, bh, 4f, Theme.cardInner)
        if (search.listening) Theme.roundRect(ctx, bx, by + bh - 1.5f, bw, 1.5f, 0f, Theme.accent)
        search.x = bx + 6f; search.y = by + 4f; search.width = bw - 12f; search.height = bh
        if (searchQuery.isEmpty() && ! search.listening) Render2D.drawString(ctx, "§8Search…", bx + 7f, by + 5f, Theme.textFaint, 1, false)
        else search.draw(ctx, mX.toFloat(), mY.toFloat())
    }

    private fun minePill(): FloatArray {
        val w = 52f
        return floatArrayOf(winX + windowWidth - pad - w, winY + bannerHeight + 6f, w, 20f)
    }

    private fun drawTabs(ctx: GuiGraphicsExtractor, mX: Int, mY: Int) {
        val tabTop = winY + bannerHeight + 6f
        Render2D.drawRect(ctx, winX + 1f, tabTop + 22f, windowWidth - 2f, 1f, Theme.borderSoft)
        tabRects().forEach { (cat, range) ->
            val sel = cat == selectedCategory
            val w = range.endInclusive - range.start
            val hov = mX >= range.start && mX <= range.endInclusive && mY >= tabTop && mY <= tabTop + 20f
            if (sel) Theme.capsule(ctx, range.start, tabTop, w, 20f, Theme.card)
            else if (hov) Theme.capsule(ctx, range.start, tabTop, w, 20f, Theme.card.withAlpha(90))
            val txt = "${if (sel) "§l" else ""}$cat"
            Render2D.drawString(ctx, txt, range.start + (w - "§l$cat".width()) / 2f, tabTop + 6f, if (sel) Theme.text else Theme.textMuted)
        }

        val p = minePill()
        val hov = mX >= p[0] && mX <= p[0] + p[2] && mY >= p[1] && mY <= p[1] + p[3]
        Theme.capsule(ctx, p[0], p[1], p[2], p[3], if (showMine) Theme.accent.withAlpha(210) else if (hov) Theme.card else Theme.cardInner)
        Theme.disc(ctx, p[0] + 9f, p[1] + 10f, 2.5f, if (showMine) darkText else Theme.textFaint)
        Render2D.drawString(ctx, "Mine", p[0] + 16f, p[1] + 6f, if (showMine) darkText else Theme.textMuted, 0.9f, false)
    }

    private fun drawList(ctx: GuiGraphicsExtractor, mX: Int, mY: Int) {
        listTop = winY + bannerHeight + tabBarHeight + 4f
        listBottom = winY + windowHeight - pad
        val viewH = listBottom - listTop
        val left = winX + pad
        val right = winX + windowWidth - pad

        val entries = RecentFeed.view(selectedCategory, searchQuery, showMine)
        val total = entries.size * rowHeight
        maxScroll = (total - viewH).coerceAtLeast(0f)
        scrollTarget = scrollTarget.coerceIn(0f, maxScroll)
        scrollAnim.update(scrollTarget)

        Resolution.scissor(ctx, left, listTop, right, listBottom)
        if (entries.isEmpty()) {
            Render2D.drawCenteredString(ctx, "§8Nothing shared yet", (left + right) / 2f, listTop + viewH / 2f - 4f, Theme.textFaint, 1, false)
        }
        else {
            val now = System.currentTimeMillis()
            var y = listTop - scrollAnim.value
            entries.forEach { e ->
                if (y + rowHeight > listTop && y < listBottom) {
                    val hov = mY >= y && mY <= y + rowHeight && mX >= left && mX <= right && mY >= listTop && mY <= listBottom
                    if (hov) Theme.roundRect(ctx, left, y + 1f, right - left, rowHeight - 2f, 4f, Theme.card.withAlpha(120))
                    Theme.capsule(ctx, left + 3f, y + 7f, 2f, rowHeight - 14f, categoryColor(e.category))
                    e.player?.let { Render2D.drawString(ctx, "§7$it", left + 12f, y + 5f, Theme.textMuted, 0.85f, false) }
                    Render2D.drawString(ctx, e.text, left + 12f, y + 15f, Theme.text, 0.9f, false)
                    val t = relativeTime(now, e.at)
                    Render2D.drawString(ctx, "§8$t", right - t.width() - 8f, y + 10f, Theme.textFaint, 0.85f, false)
                }
                y += rowHeight
            }
        }
        ctx.disableScissor()

        if (maxScroll > 0f) {
            val barX = right - 2f
            val thumbH = ((viewH / total) * viewH).coerceAtLeast(24f)
            val thumbY = listTop + (scrollAnim.value / maxScroll) * (viewH - thumbH)
            Render2D.drawRect(ctx, barX, listTop, 2f, viewH, Theme.cardInner)
            Render2D.drawRect(ctx, barX, thumbY, 2f, thumbH, Theme.accent.withAlpha(170))
        }
    }

    private fun categoryColor(category: String) = when (category) {
        "Events" -> Theme.accent
        "Rabbits" -> java.awt.Color(120, 200, 235)
        else -> java.awt.Color(150, 200, 120)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val mx = Resolution.getMouseX(event.x)
        val my = Resolution.getMouseY(event.y)

        if (search.mouseClicked(mx.toFloat(), my.toFloat(), event)) { scrollTarget = 0f; return true }

        val tabTop = winY + bannerHeight + 6f
        val p = minePill()
        if (mx >= p[0] && mx <= p[0] + p[2] && my >= p[1] && my <= p[1] + p[3] && event.button() == 0) {
            showMine = ! showMine
            scrollTarget = 0f; scrollAnim.set(0f)
            Style.playClickSound(if (showMine) 1.05f else 0.95f)
            return true
        }
        if (my >= tabTop && my <= tabTop + 20f && event.button() == 0) {
            tabRects().firstOrNull { mx >= it.second.start && mx <= it.second.endInclusive }?.let {
                selectedCategory = it.first
                scrollTarget = 0f; scrollAnim.set(0f)
                Style.playClickSound(1f)
                return true
            }
        }
        if (mx >= winX && mx <= winX + windowWidth && my >= winY && my <= winY + windowHeight) { search.resetState(); return true }
        return super.mouseClicked(event, doubled)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean { search.mouseReleased(); return super.mouseReleased(event) }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        if (Resolution.getMouseX(mouseX) in winX.toInt()..(winX + windowWidth).toInt()) {
            scrollTarget = (scrollTarget - (vertical * 26).toFloat()).coerceIn(0f, maxScroll)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (search.keyTyped(event)) { scrollTarget = 0f; return true }
        return super.charTyped(event)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (search.keyPressed(event)) { scrollTarget = 0f; return true }
        if (event.key == InputConstants.KEY_ESCAPE && search.listening) { search.resetState(); return true }
        return super.keyPressed(event)
    }

    override fun onClose() {
        needsOpenAnim = true
        search.resetState()
        searchQuery = ""
        val parent = modMenuParent
        modMenuParent = null
        if (parent != null) { SkyblockConnect.mc.setScreenAndShow(parent); return }
        super.onClose()
    }

    override fun isPauseScreen() = false
}