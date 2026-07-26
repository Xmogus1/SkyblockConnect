package com.skyblockconnect.ui.gui

import com.skyblockconnect.SkyblockConnect
import com.skyblockconnect.party.PartyFinder
import com.skyblockconnect.ui.clickgui.components.Style
import com.skyblockconnect.ui.utils.Animation
import com.skyblockconnect.ui.utils.Resolution
import com.skyblockconnect.ui.utils.TextInputHandler
import com.skyblockconnect.utils.ColorUtils.withAlpha
import com.skyblockconnect.utils.render.Render2D
import com.skyblockconnect.utils.render.Render2D.width
import com.skyblockconnect.websocket.SbcNet
import com.skyblockconnect.websocket.SbcSocket
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.awt.Color

object PartyFinderScreen: Screen(Component.literal("SBC Party Finder")) {

    private const val windowWidth = 560f
    private const val windowHeight = 384f
    private const val bannerHeight = 46f
    private const val fieldH = 20f
    private const val rowHeight = 34f
    private const val pad = 16f

    private val darkText = Color(20, 18, 12)

    private var winX = 0f
    private var winY = 0f
    private var listTop = 0f
    private var listBottom = 0f

    private var scrollTarget = 0f
    private var maxScroll = 0f
    private val scrollAnim = Animation(220L)
    private val openAnim = Animation(240L)
    private var needsOpenAnim = true

    private var pickerStep = 0
    private var pickerSkill: String? = null

    private var expandedLeader: String? = null
    private const val expandH = 26f

    private val noteField = TextInputHandler({ PartyFinder.note }, { PartyFinder.note = it })
    private val levelField = TextInputHandler(
        { if (PartyFinder.minLevel > 0) PartyFinder.minLevel.toString() else "" },
        { PartyFinder.minLevel = it.filter(Char::isDigit).take(4).toIntOrNull() ?: 0 },
    )
    private val fields get() = listOf(noteField, levelField)

    @JvmField
    var modMenuParent: Screen? = null

    private fun focusOnly(target: TextInputHandler?) = fields.forEach { if (it !== target) it.resetState() }

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
        drawBanner(ctx)
        drawForm(ctx, mX, mY)
        drawList(ctx, mX, mY)
        if (pickerStep != 0) drawPicker(ctx, mX, mY)

        if (popping) pose.popMatrix()
        Resolution.pop(ctx)
    }

    private fun drawBanner(ctx: GuiGraphicsExtractor) {
        Theme.roundedTopBand(ctx, winX + 1f, winY + 1f, windowWidth - 2f, bannerHeight, 6f, Theme.bannerTop, Theme.bannerBottom)
        Render2D.drawRect(ctx, winX + 1f, winY + bannerHeight, windowWidth - 2f, 1f, Theme.accent.withAlpha(210))
        Theme.disc(ctx, winX + pad + 6f, winY + 16f, 5f, Theme.accent)
        Render2D.drawString(ctx, "§lPARTY FINDER", winX + pad + 16f, winY + 11f, Theme.text)
        Render2D.drawString(ctx, "find or list parties", winX + pad + 16f, winY + 25f, Theme.textFaint, 0.85f, false)

        val (dot, label) = when {
            ! SbcSocket.connected -> Theme.offline to "Offline"
            PartyFinder.listed -> Theme.online to "Listed"
            PartyFinder.inParty -> Theme.online to "In a party"
            else -> Theme.pending to "Ready"
        }
        val pw = label.width() + 24f
        Theme.statusPill(ctx, winX + windowWidth - pad - pw, winY + 15f, dot, label)
    }

    private fun formTop() = winY + bannerHeight + 10f
    private fun fieldRowY() = formTop() + 16f
    private fun chipRowY() = fieldRowY() + fieldH + 8f
    private fun formBottom() = chipRowY() + 22f

    private val noteX get() = winX + pad
    private val noteW = 250f
    private val levelX get() = noteX + noteW + 8f
    private val levelW = 70f
    private fun listBtn(): FloatArray { val bw = 96f; return floatArrayOf(winX + windowWidth - pad - bw, fieldRowY(), bw, fieldH) }

    private val slotsLabelX get() = levelX + levelW + 8f
    private fun slotsMinus() = floatArrayOf(slotsLabelX + 30f, fieldRowY() + 1f, 18f, 18f)
    private fun slotsPlus() = floatArrayOf(slotsLabelX + 70f, fieldRowY() + 1f, 18f, 18f)

    private fun drawStep(ctx: GuiGraphicsExtractor, b: FloatArray, label: String, mX: Int, mY: Int) {
        val hov = mX >= b[0] && mX <= b[0] + b[2] && mY >= b[1] && mY <= b[1] + b[3]
        Theme.roundRect(ctx, b[0], b[1], b[2], b[3], 4f, if (hov) Theme.cardHover else Theme.cardInner)
        Render2D.drawCenteredString(ctx, "§f$label", b[0] + b[2] / 2f, b[1] + b[3] / 2f - 4f, Theme.textMuted, 1, false)
    }

    private fun drawForm(ctx: GuiGraphicsExtractor, mX: Int, mY: Int) {
        Render2D.drawString(ctx, "§lYour party", winX + pad, formTop() - 1f, Theme.textMuted, 0.9f, false)

        val fy = fieldRowY()

        Theme.roundRect(ctx, noteX, fy, noteW, fieldH, 4f, Theme.cardInner)
        noteField.x = noteX + 3f; noteField.y = fy + 1f; noteField.width = noteW - 6f; noteField.height = fieldH
        if (PartyFinder.note.isEmpty() && ! noteField.listening) Render2D.drawString(ctx, "§8Note (what you're doing)", noteX + 7f, fy + 6f, Theme.textFaint, 0.85f, false)
        else noteField.draw(ctx, mX.toFloat(), mY.toFloat())
        if (noteField.listening) Render2D.drawRect(ctx, noteX, fy + fieldH - 1.5f, noteW, 1.5f, Theme.accent)

        Theme.roundRect(ctx, levelX, fy, levelW, fieldH, 4f, Theme.cardInner)
        levelField.x = levelX + 3f; levelField.y = fy + 1f; levelField.width = levelW - 6f; levelField.height = fieldH
        if (PartyFinder.minLevel <= 0 && ! levelField.listening) Render2D.drawString(ctx, "§8SB lvl", levelX + 7f, fy + 6f, Theme.textFaint, 0.85f, false)
        else levelField.draw(ctx, mX.toFloat(), mY.toFloat())
        if (levelField.listening) Render2D.drawRect(ctx, levelX, fy + fieldH - 1.5f, levelW, 1.5f, Theme.accent)

        val b = listBtn()
        val enabled = PartyFinder.listed || PartyFinder.canList()
        val hovered = enabled && mX >= b[0] && mX <= b[0] + b[2] && mY >= b[1] && mY <= b[1] + b[3]
        val col = when { PartyFinder.listed -> Theme.offline; enabled -> Theme.accent; else -> Theme.borderSoft }
        Theme.roundRect(ctx, b[0], b[1], b[2], b[3], 4f, if (hovered) col else col.withAlpha(if (enabled) 210 else 255))
        val labelC = if (enabled) darkText else Theme.textFaint
        Render2D.drawCenteredString(ctx, "§l${if (PartyFinder.listed) "Unlist" else "List Party"}", b[0] + b[2] / 2f, b[1] + b[3] / 2f - 4f, labelC, 1, false)

        Render2D.drawString(ctx, "§7Slots", slotsLabelX, fy + 6f, Theme.textFaint, 0.85f, false)
        val mn = slotsMinus(); val pl = slotsPlus()
        drawStep(ctx, mn, "−", mX, mY)
        drawStep(ctx, pl, "+", mX, mY)
        Render2D.drawCenteredString(ctx, "§f§l${PartyFinder.slots}", (mn[0] + mn[2] + pl[0]) / 2f, fy + 6f, Theme.text, 1, false)

        var cx = winX + pad
        val cy = chipRowY()
        Render2D.drawString(ctx, "§7Reqs:", cx, cy + 4f, Theme.textFaint, 0.85f, false)
        cx += 34f
        PartyFinder.skillReqs.forEach { (skill, lvl) ->
            val label = "$skill $lvl"
            val w = label.width() + 22f
            Theme.roundRect(ctx, cx, cy, w, 16f, 4f, Theme.card)
            Render2D.drawString(ctx, "§f$label", cx + 6f, cy + 4f, Theme.text, 0.85f, false)
            Render2D.drawString(ctx, "§c✕", cx + w - 11f, cy + 4f, Theme.offline, 0.85f, false)
            cx += w + 5f
        }

        val addW = 58f
        val addHov = mX >= cx && mX <= cx + addW && mY >= cy && mY <= cy + 16f
        Theme.roundRect(ctx, cx, cy, addW, 16f, 4f, if (addHov) Theme.cardHover else Theme.cardInner)
        Render2D.drawString(ctx, "§a+ §7skill", cx + 6f, cy + 4f, Theme.textMuted, 0.85f, false)

        Render2D.drawRect(ctx, winX + 1f, formBottom(), windowWidth - 2f, 1f, Theme.borderSoft)
    }

    private fun drawList(ctx: GuiGraphicsExtractor, mX: Int, mY: Int) {
        listTop = formBottom() + 16f
        listBottom = winY + windowHeight - pad
        val viewH = listBottom - listTop
        val left = winX + pad
        val right = winX + windowWidth - pad

        val listings = PartyFinder.listings
        val totalH = listings.sumOf { rowHeightOf(it).toDouble() }.toFloat()
        maxScroll = (totalH - viewH).coerceAtLeast(0f)
        scrollTarget = scrollTarget.coerceIn(0f, maxScroll)
        scrollAnim.update(scrollTarget)

        Render2D.drawString(ctx, "§lOpen parties §7(${listings.size})", left, listTop - 12f, Theme.textMuted, 0.9f, false)

        Resolution.scissor(ctx, left, listTop, right, listBottom)
        if (listings.isEmpty()) {
            Render2D.drawCenteredString(ctx, "§8No parties listed right now", (left + right) / 2f, listTop + viewH / 2f - 4f, Theme.textFaint, 1, false)
        }
        else {
            var y = listTop - scrollAnim.value
            listings.forEach { l ->
                val rh = rowHeightOf(l)
                if (y + rh > listTop && y < listBottom) drawListing(ctx, l, left, y, right - left, mX, mY)
                y += rh
            }
        }
        ctx.disableScissor()

        if (maxScroll > 0f) {
            val barX = right - 2f
            val thumbH = ((viewH / totalH) * viewH).coerceAtLeast(24f)
            val thumbY = listTop + (scrollAnim.value / maxScroll) * (viewH - thumbH)
            Render2D.drawRect(ctx, barX, listTop, 2f, viewH, Theme.cardInner)
            Render2D.drawRect(ctx, barX, thumbY, 2f, thumbH, Theme.accent.withAlpha(170))
        }
    }

    private fun rowHeightOf(l: PartyFinder.Listing): Float =
        rowHeight + if (l.leader == expandedLeader) expandH else 0f

    private fun drawListing(ctx: GuiGraphicsExtractor, l: PartyFinder.Listing, x: Float, y: Float, w: Float, mX: Int, mY: Int) {
        val expanded = l.leader == expandedLeader
        val fullH = rowHeightOf(l)
        val hovered = mY >= y && mY <= y + rowHeight - 2f && mX >= x && mX <= x + w && mY >= listTop && mY <= listBottom
        Theme.roundRect(ctx, x, y + 1f, w, fullH - 2f, 4f, if (hovered || expanded) Theme.cardHover else Theme.card)
        Theme.capsule(ctx, x + 3f, y + 8f, 2f, rowHeight - 18f, Theme.accent)

        val chev = if (expanded) "§7▾" else "§8▸"
        Render2D.drawString(ctx, chev, x + 12f, y + 6f, Theme.textFaint, 0.9f, false)

        val title = if (l.note.isNotEmpty()) l.note else "${l.leader}'s party"
        Render2D.drawString(ctx, "§f§l$title", x + 20f, y + 6f, Theme.text)

        val count = l.members.size.coerceAtLeast(1)
        val sub = buildString {
            append("§7").append(l.leader)
            if (l.minLevel > 0) append("  §8· §7SB §f${l.minLevel}+")
            if (l.reqs.isNotEmpty()) append("  §8· §7").append(l.reqs)
            if (l.slots > 0) append("  §8· §f").append(count).append("§7/§f").append(l.slots)
        }
        Render2D.drawString(ctx, sub, x + 20f, y + 17f, Theme.textMuted, 0.85f, false)

        if (expanded) {
            val my2 = y + rowHeight
            Render2D.drawRect(ctx, x + 10f, my2, w - 20f, 1f, Theme.borderSoft)
            val who = if (l.members.isEmpty()) "§8(members not shared)"
            else "§7Members: §f" + l.members.joinToString("§7, §f")
            Render2D.drawString(ctx, who, x + 12f, my2 + 7f, Theme.textMuted, 0.85f, false)
        }

        val bw = 60f; val bh = 20f
        val bx = x + w - bw - 8f; val by = y + (rowHeight - bh) / 2f
        val bHov = mX >= bx && mX <= bx + bw && mY >= by && mY <= by + bh && mY >= listTop && mY <= listBottom
        val mine = l.leader.equals(SbcNet.selfName, ignoreCase = true)
        val col = if (mine) Theme.borderSoft else if (bHov) Theme.accent else Theme.accent.withAlpha(210)
        Theme.roundRect(ctx, bx, by, bw, bh, 5f, col)
        Render2D.drawCenteredString(ctx, if (mine) "§8you" else "§lJoin", bx + bw / 2f, by + bh / 2f - 4f, if (mine) Theme.textFaint else darkText, 1, false)
    }

    private fun pickerRect(): FloatArray {
        val pw = 320f; val ph = 210f
        return floatArrayOf(winX + (windowWidth - pw) / 2f, winY + (windowHeight - ph) / 2f, pw, ph)
    }

    private fun drawPicker(ctx: GuiGraphicsExtractor, mX: Int, mY: Int) {
        Render2D.drawRect(ctx, winX + 1f, winY + 1f, windowWidth - 2f, windowHeight - 2f, Color(0, 0, 0, 150))
        val p = pickerRect()
        Theme.roundRectRing(ctx, p[0], p[1], p[2], p[3], 6f, Theme.windowBottom, Theme.border)
        val title = if (pickerStep == 1) "Choose a skill" else "${pickerSkill}: choose a level"
        Render2D.drawString(ctx, "§l$title", p[0] + 12f, p[1] + 10f, Theme.text)
        Render2D.drawString(ctx, "§8esc to cancel", p[0] + p[2] - 66f, p[1] + 11f, Theme.textFaint, 0.85f, false)

        if (pickerStep == 1) {
            val cols = 3; val cw = (p[2] - 24f) / cols; val ch = 22f
            PartyFinder.SKILLS.keys.forEachIndexed { i, skill ->
                val bx = p[0] + 12f + (i % cols) * cw
                val by = p[1] + 30f + (i / cols) * (ch + 4f)
                val hov = mX >= bx && mX <= bx + cw - 4f && mY >= by && mY <= by + ch
                Theme.roundRect(ctx, bx, by, cw - 4f, ch, 4f, if (hov) Theme.cardHover else Theme.card)
                Render2D.drawCenteredString(ctx, skill, bx + (cw - 4f) / 2f, by + ch / 2f - 4f, Theme.text, 0.9f, false)
            }
        }
        else {
            val max = PartyFinder.SKILLS[pickerSkill] ?: 60
            val cols = 10; val cw = (p[2] - 24f) / cols; val ch = 16f
            for (lvl in 1..max) {
                val bx = p[0] + 12f + ((lvl - 1) % cols) * cw
                val by = p[1] + 30f + ((lvl - 1) / cols) * (ch + 3f)
                val hov = mX >= bx && mX <= bx + cw - 3f && mY >= by && mY <= by + ch
                Theme.roundRect(ctx, bx, by, cw - 3f, ch, 3f, if (hov) Theme.accent.withAlpha(200) else Theme.card)
                Render2D.drawCenteredString(ctx, "$lvl", bx + (cw - 3f) / 2f, by + ch / 2f - 4f, if (hov) darkText else Theme.textMuted, 0.85f, false)
            }
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val mx = Resolution.getMouseX(event.x).toFloat()
        val my = Resolution.getMouseY(event.y).toFloat()

        if (pickerStep != 0) { handlePickerClick(mx, my); return true }

        if (noteField.mouseClicked(mx, my, event)) { focusOnly(noteField); return true }
        if (levelField.mouseClicked(mx, my, event)) { focusOnly(levelField); return true }

        val b = listBtn()
        if (mx >= b[0] && mx <= b[0] + b[2] && my >= b[1] && my <= b[1] + b[3] && event.button() == 0) {
            focusOnly(null); PartyFinder.toggleListed(); Style.playClickSound(1f); return true
        }

        if (event.button() == 0) {
            val mn = slotsMinus(); val pl = slotsPlus()
            if (mx >= mn[0] && mx <= mn[0] + mn[2] && my >= mn[1] && my <= mn[1] + mn[3]) {
                focusOnly(null); PartyFinder.bumpSlots(-1); Style.playClickSound(0.9f); return true
            }
            if (mx >= pl[0] && mx <= pl[0] + pl[2] && my >= pl[1] && my <= pl[1] + pl[3]) {
                focusOnly(null); PartyFinder.bumpSlots(1); Style.playClickSound(1.1f); return true
            }
        }

        if (my >= chipRowY() && my <= chipRowY() + 16f) {
            var cx = winX + pad + 34f
            val toRemove = PartyFinder.skillReqs.entries.firstOrNull { (skill, lvl) ->
                val w = "$skill $lvl".width() + 22f
                val hit = mx >= cx && mx <= cx + w
                cx += w + 5f
                hit
            }
            if (toRemove != null) { PartyFinder.skillReqs.remove(toRemove.key); Style.playClickSound(0.9f); return true }
            if (mx >= cx && mx <= cx + 58f) { pickerStep = 1; focusOnly(null); Style.playClickSound(1f); return true }
        }

        if (my >= listTop && my <= listBottom) {
            var y = listTop - scrollAnim.value
            val right = winX + windowWidth - pad
            for (l in PartyFinder.listings) {
                val rh = rowHeightOf(l)
                val bx = right - 68f; val by = y + (rowHeight - 20f) / 2f
                if (mx >= bx && mx <= bx + 60f && my >= by && my <= by + 20f && event.button() == 0) {
                    focusOnly(null); PartyFinder.join(l); Style.playClickSound(1.05f); return true
                }

                if (my >= y && my <= y + rowHeight - 2f && mx >= winX + pad && mx <= right && event.button() == 0) {
                    focusOnly(null)
                    expandedLeader = if (expandedLeader == l.leader) null else l.leader
                    Style.playClickSound(0.85f); return true
                }
                y += rh
            }
        }

        focusOnly(null)
        return if (mx >= winX && mx <= winX + windowWidth && my >= winY && my <= winY + windowHeight) true
        else super.mouseClicked(event, doubled)
    }

    private fun handlePickerClick(mx: Float, my: Float) {
        val p = pickerRect()
        if (mx < p[0] || mx > p[0] + p[2] || my < p[1] || my > p[1] + p[3]) { pickerStep = 0; pickerSkill = null; return }

        if (pickerStep == 1) {
            val cols = 3; val cw = (p[2] - 24f) / cols; val ch = 22f
            PartyFinder.SKILLS.keys.forEachIndexed { i, skill ->
                val bx = p[0] + 12f + (i % cols) * cw
                val by = p[1] + 30f + (i / cols) * (ch + 4f)
                if (mx >= bx && mx <= bx + cw - 4f && my >= by && my <= by + ch) { pickerSkill = skill; pickerStep = 2; Style.playClickSound(1f) }
            }
        }
        else {
            val max = PartyFinder.SKILLS[pickerSkill] ?: 60
            val cols = 10; val cw = (p[2] - 24f) / cols; val ch = 16f
            for (lvl in 1..max) {
                val bx = p[0] + 12f + ((lvl - 1) % cols) * cw
                val by = p[1] + 30f + ((lvl - 1) / cols) * (ch + 3f)
                if (mx >= bx && mx <= bx + cw - 3f && my >= by && my <= by + ch) {
                    pickerSkill?.let { PartyFinder.skillReqs[it] = lvl }
                    pickerStep = 0; pickerSkill = null; Style.playClickSound(1.1f); return
                }
            }
        }
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean { fields.forEach { it.mouseReleased() }; return super.mouseReleased(event) }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        if (pickerStep == 0 && Resolution.getMouseX(mouseX) in winX.toInt()..(winX + windowWidth).toInt()) {
            scrollTarget = (scrollTarget - (vertical * 26).toFloat()).coerceIn(0f, maxScroll)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (pickerStep == 0) fields.firstOrNull { it.listening }?.let { if (it.keyTyped(event)) return true }
        return super.charTyped(event)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key == InputConstants.KEY_ESCAPE) {
            if (pickerStep != 0) { pickerStep = 0; pickerSkill = null; return true }
            if (fields.any { it.listening }) { focusOnly(null); return true }
        }
        if (pickerStep == 0) fields.firstOrNull { it.listening }?.let { if (it.keyPressed(event)) return true }
        return super.keyPressed(event)
    }

    override fun onClose() {
        needsOpenAnim = true
        pickerStep = 0; pickerSkill = null
        focusOnly(null)
        val parent = modMenuParent
        modMenuParent = null
        if (parent != null) { SkyblockConnect.mc.setScreenAndShow(parent); return }
        super.onClose()
    }

    override fun isPauseScreen() = false
}