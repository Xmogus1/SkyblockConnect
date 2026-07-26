package com.skyblockconnect.ui.gui

import com.skyblockconnect.SkyblockConnect
import com.skyblockconnect.config.Config
import com.skyblockconnect.features.Feature
import com.skyblockconnect.features.FeatureManager
import com.skyblockconnect.features.impl.general.Connection
import com.skyblockconnect.features.impl.general.Interface
import com.skyblockconnect.ui.clickgui.TooltipManager
import com.skyblockconnect.ui.clickgui.components.Style
import com.skyblockconnect.ui.clickgui.enums.CategoryType
import com.skyblockconnect.ui.utils.Animation
import com.skyblockconnect.ui.utils.Resolution
import com.skyblockconnect.utils.ColorUtils.lerp
import com.skyblockconnect.utils.ColorUtils.withAlpha
import com.skyblockconnect.utils.render.Render2D
import com.skyblockconnect.utils.render.Render2D.width
import com.skyblockconnect.websocket.SbcSocket
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

object SbcScreen: Screen(Component.literal("Skyblock Connect")) {

    private const val windowWidth = 540f
    private const val windowHeight = 372f
    private const val bannerHeight = 58f
    private const val tabBarHeight = 34f
    private const val tabHeight = 24f
    private const val rowHeight = 42f
    private const val rowGap = 6f
    private const val pad = 16f

    private var winX = 0f
    private var winY = 0f

    private var contentTop = 0f
    private var contentBottom = 0f
    private var contentLeft = 0f
    private var contentRight = 0f

    private val categories: List<CategoryType>
        get() = CategoryType.entries.filter { FeatureManager.getFeaturesByCategory(it).isNotEmpty() }

    private var selected: CategoryType? = null
    private val expanded = mutableSetOf<Feature>()

    private var scrollTarget = 0f
    private var maxScroll = 0f
    private val scrollAnim = Animation(260L)

    private val openAnim = Animation(260L)
    private var needsOpenAnim = true
    private val tabAnim = Animation(260L)
    private val switchAnim = Animation(240L, 1f)
    private val expandAnims = mutableMapOf<Feature, Animation>()
    private val hoverAnims = mutableMapOf<Feature, Animation>()
    private val toggleAnims = mutableMapOf<Feature, Animation>()

    @JvmField
    var modMenuParent: Screen? = null

    private fun icon(category: CategoryType) = when (category) {
        CategoryType.EVENTS -> "✦"
        CategoryType.ACHIEVEMENTS -> "❃"
        CategoryType.GENERAL -> "⚙"
        CategoryType.DEV -> "⚑"
    }

    private fun label(category: CategoryType) =
        category.name.lowercase().replaceFirstChar { it.uppercase() }

    private fun featuresOf(category: CategoryType?) =
        category?.let { FeatureManager.getFeaturesByCategory(it).sortedBy { f -> f.name } }.orEmpty()

    private fun visibleSettings(feature: Feature) = feature.configSettings.filter { it.visibility.invoke() }

    private fun ensureSelected() {
        val cats = categories
        if (cats.isEmpty()) selected = null
        else if (selected == null || selected !in cats) selected = cats.first()
    }

    private fun tabText(category: CategoryType) = "§l${icon(category)} ${label(category)}"

    private fun tabRects(): List<Pair<CategoryType, ClosedFloatingPointRange<Float>>> {
        val out = mutableListOf<Pair<CategoryType, ClosedFloatingPointRange<Float>>>()
        var tx = winX + pad
        categories.forEach { category ->

            val tabW = tabText(category).width().toFloat() + 24f
            out += category to (tx..(tx + tabW))
            tx += tabW + 6f
        }
        return out
    }

    override fun extractRenderState(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        Resolution.refresh()
        Resolution.push(ctx)
        val mX = Resolution.getMouseX(mouseX.toDouble())
        val mY = Resolution.getMouseY(mouseY.toDouble())

        ensureSelected()
        TooltipManager.reset()

        if (needsOpenAnim) {
            openAnim.set(0f)
            needsOpenAnim = false
        }
        openAnim.update(1f)
        switchAnim.update(1f)
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
            pose.pushMatrix()
            pose.translate(px, py)
            pose.scale(scale)
            pose.translate(-px, -py)
        }

        Theme.roundRectRing(ctx, winX, winY, windowWidth, windowHeight, 7f, Theme.windowBottom, Theme.border)

        drawBanner(ctx)
        drawTabs(ctx, mX, mY)
        drawContent(ctx, mX, mY)

        if (popping) pose.popMatrix()

        TooltipManager.draw(ctx, Resolution.width, Resolution.height)
        Resolution.pop(ctx)
    }

    private fun drawBanner(ctx: GuiGraphicsExtractor) {

        Theme.roundedTopBand(ctx, winX + 1f, winY + 1f, windowWidth - 2f, bannerHeight, 6f, Theme.bannerTop, Theme.bannerBottom)
        Render2D.drawRect(ctx, winX + 1f, winY + bannerHeight, windowWidth - 2f, 1f, Theme.accent.withAlpha(210))
        Theme.glowUnder(ctx, winX + 1f, winY + bannerHeight + 1f, windowWidth - 2f, 12f, Theme.accent)

        val markX = winX + pad + 6f
        val markY = winY + 20f
        Theme.disc(ctx, markX, markY, 6f, Theme.accent)
        Theme.disc(ctx, markX, markY, 3f, Theme.bannerTop)
        Render2D.drawString(ctx, "§lSKYBLOCK §r§lCONNECT", markX + 14f, winY + 15f, Theme.text)
        Render2D.drawString(ctx, "toggle stuff", markX + 14f, winY + 30f, Theme.textFaint, 0.85f, false)

        val (dot, statusLabel) = when {
            ! Connection.enabled -> Theme.offline to "Relay off"
            SbcSocket.connected -> Theme.online to "Connected to server"
            else -> Theme.pending to "Connecting…"
        }
        val pillWidth = statusLabel.width() + 24f
        Theme.statusPill(ctx, winX + windowWidth - pad - pillWidth, winY + 14f, dot, statusLabel)

        val version = runCatching { SkyblockConnect.MOD_VERSION }.getOrDefault("")
        if (version.isNotEmpty()) {
            Render2D.drawString(ctx, "§8v$version", winX + windowWidth - pad - version.length * 6f - 6f, winY + 36f, Theme.textFaint, 0.85f, false)
        }
    }

    private fun drawTabs(ctx: GuiGraphicsExtractor, mX: Int, mY: Int) {
        val tabTop = winY + bannerHeight + 8f
        Render2D.drawRect(ctx, winX + 1f, tabTop + tabHeight + 4f, windowWidth - 2f, 1f, Theme.borderSoft)

        var selectedMid = -1f
        var selectedW = 0f
        tabRects().forEach { (category, range) ->
            val isSelected = category == selected
            val hovered = mX >= range.start && mX <= range.endInclusive && mY >= tabTop && mY <= tabTop + tabHeight
            val w = range.endInclusive - range.start

            when {
                isSelected -> Theme.capsule(ctx, range.start, tabTop, w, tabHeight, Theme.card)
                hovered -> Theme.capsule(ctx, range.start, tabTop, w, tabHeight, Theme.card.withAlpha(90))
            }

            val tint = if (isSelected) Theme.text else Theme.textMuted
            val plain = "${icon(category)} ${label(category)}"
            val textW = tabText(category).width().toFloat()
            val textX = range.start + (w - textW) / 2f
            Render2D.drawString(ctx, if (isSelected) "§l$plain" else plain, textX, tabTop + 8f, tint)

            if (isSelected) {
                selectedMid = range.start + w / 2f
                selectedW = w
            }
        }

        if (selectedMid >= 0f) {
            val underlineW = (selectedW - 20f).coerceAtLeast(10f)
            if (openAnim.value < 0.05f) tabAnim.set(selectedMid) else tabAnim.update(selectedMid)
            Theme.capsule(ctx, tabAnim.value - underlineW / 2f, tabTop + tabHeight + 3f, underlineW, 3f, Theme.accent)
        }
    }

    private fun drawContent(ctx: GuiGraphicsExtractor, mX: Int, mY: Int) {
        contentLeft = winX + pad
        contentRight = winX + windowWidth - pad
        contentTop = winY + bannerHeight + tabBarHeight + 8f
        contentBottom = winY + windowHeight - pad
        val viewW = (contentRight - contentLeft).coerceAtLeast(80f)
        val viewH = (contentBottom - contentTop).coerceAtLeast(40f)

        val features = featuresOf(selected)

        features.forEach { feature ->
            val target = if (feature in expanded) 1f else 0f
            expandAnims.getOrPut(feature) { Animation(240L, target) }.update(target)
        }

        var total = 0f
        features.forEach { total += rowHeight + expandedHeightAnimated(it) + rowGap }
        if (total > 0f) total -= rowGap

        maxScroll = (total - viewH).coerceAtLeast(0f)
        scrollTarget = scrollTarget.coerceIn(0f, maxScroll)
        scrollAnim.update(scrollTarget)
        if (scrollAnim.value > maxScroll) scrollAnim.set(maxScroll)

        val rowWidth = viewW - (if (maxScroll > 0f) 7f else 0f)

        Resolution.scissor(ctx, contentLeft, contentTop, contentRight, contentBottom)

        if (features.isEmpty()) {
            Render2D.drawCenteredString(
                ctx, "§8Nothing here yet",
                contentLeft + viewW / 2f, contentTop + viewH / 2f - 4f, Theme.textFaint, 1, false,
            )
        }
        else {
            val slide = (1f - switchAnim.value) * 18f
            var y = contentTop - scrollAnim.value
            features.forEach { feature ->
                val expH = expandedHeightAnimated(feature)
                if (y + rowHeight + expH > contentTop && y < contentBottom) {
                    drawRow(ctx, feature, contentLeft + slide, y, rowWidth, mX, mY)
                    if (expH > 0.5f) drawSettings(ctx, feature, contentLeft + slide, y + rowHeight, rowWidth, expH, mX, mY)
                }
                y += rowHeight + expH + rowGap
            }
        }

        ctx.disableScissor()

        if (maxScroll > 0f) {
            val barX = contentRight - 3f
            val thumbH = ((viewH / total) * viewH).coerceAtLeast(24f)
            val travel = (viewH - thumbH).coerceAtLeast(0f)
            val thumbY = contentTop + (scrollAnim.value / maxScroll) * travel
            Render2D.drawRect(ctx, barX, contentTop, 3f, viewH, Theme.cardInner)
            Render2D.drawRect(ctx, barX, thumbY, 3f, thumbH, Theme.accent.withAlpha(170))
        }
    }

    private const val toggleWidth = 32f
    private const val toggleHeight = 16f

    private fun drawRow(ctx: GuiGraphicsExtractor, feature: Feature, x: Float, y: Float, w: Float, mX: Int, mY: Int) {
        val hovered = mX >= x && mX <= x + w && mY >= y && mY <= y + rowHeight && mY >= contentTop && mY <= contentBottom
        val hoverAnim = hoverAnims.getOrPut(feature) { Animation(150L) }
        hoverAnim.update(if (hovered) 1f else 0f)

        val isExpanded = feature in expanded
        val fill = Theme.card.lerp(Theme.cardHover, hoverAnim.value)
        if (isExpanded) Theme.roundRectRing(ctx, x, y, w, rowHeight, 5f, fill, Theme.accentDim)
        else Theme.roundRect(ctx, x, y, w, rowHeight, 5f, fill)

        val spine = if (feature.enabled) Theme.accent else Theme.borderSoft
        Render2D.drawRect(ctx, x + 4f, y + 9f, 3f, rowHeight - 18f, spine)

        Render2D.drawString(ctx, "§l${feature.name}", x + 15f, y + 9f, Theme.text)
        feature.description?.let {
            Render2D.drawString(ctx, it, x + 15f, y + 24f, Theme.textFaint, 0.85f, false)
        }

        if (feature !== Interface && feature !== Connection) {
            val anim = toggleAnims.getOrPut(feature) { Animation(220L, if (feature.enabled) 1f else 0f) }
            anim.update(if (feature.enabled) 1f else 0f)
            Theme.toggleSwitch(ctx, x + w - toggleWidth - 12f, y + (rowHeight - toggleHeight) / 2f, toggleWidth, toggleHeight, anim.value)
        }
    }

    private fun expandedHeight(feature: Feature): Float {
        val settings = visibleSettings(feature)
        if (settings.isEmpty()) return 26f
        return settings.sumOf { it.height }.toFloat() + 16f
    }

    private fun expandedHeightAnimated(feature: Feature): Float {
        val p = expandAnims[feature]?.value ?: 0f
        return if (p <= 0.001f) 0f else expandedHeight(feature) * p
    }

    private fun drawSettings(ctx: GuiGraphicsExtractor, feature: Feature, x: Float, y: Float, w: Float, h: Float, mX: Int, mY: Int) {
        Theme.roundRect(ctx, x, y, w, h, 5f, Theme.cardInner)
        Theme.capsule(ctx, x + 3f, y + 4f, 3f, h - 8f, Theme.accent.withAlpha(120))

        Resolution.scissor(
            ctx,
            maxOf(x, contentLeft), maxOf(y, contentTop),
            minOf(x + w, contentRight), minOf(y + h, contentBottom),
        )

        val settings = visibleSettings(feature)
        if (settings.isEmpty()) {
            Render2D.drawString(ctx, "§8No settings", x + 16f, y + 9f, Theme.textFaint, 1, false)
            ctx.disableScissor()
            return
        }

        val sx = (x + 14f).toInt()
        val sw = (w - 24f).toInt().coerceAtLeast(60)
        var sy = y + 8f

        settings.forEach { setting ->
            setting.x = sx
            setting.y = sy.toInt()
            setting.width = sw
            setting.draw(ctx, mX, mY)

            val hovered = mX >= setting.x && mX <= setting.x + setting.width &&
                mY >= setting.y && mY <= setting.y + setting.height &&
                mY >= contentTop && mY <= contentBottom
            if (hovered) TooltipManager.hover(setting.description, mX, mY)

            sy += setting.height
        }

        ctx.disableScissor()
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        val mx = Resolution.getMouseX(event.x)
        val my = Resolution.getMouseY(event.y)
        val button = event.button()

        if (insideContent(mx, my)) {
            for (feature in featuresOf(selected)) {
                if (feature !in expanded) continue
                for (setting in visibleSettings(feature)) {
                    if (setting.mouseClicked(mx.toDouble(), my.toDouble(), button)) return true
                }
            }
        }

        val tabTop = winY + bannerHeight + 8f
        if (my >= tabTop && my <= tabTop + tabHeight && button == 0) {
            tabRects().firstOrNull { mx >= it.second.start && mx <= it.second.endInclusive }?.let { (category, _) ->
                if (selected != category) {
                    selected = category
                    scrollTarget = 0f
                    scrollAnim.set(0f)
                    switchAnim.set(0f)
                }
                Style.playClickSound(1f)
                return true
            }
        }

        if (insideContent(mx, my)) {
            val rowWidth = (contentRight - contentLeft) - (if (maxScroll > 0f) 7f else 0f)
            var y = contentTop - scrollAnim.value
            for (feature in featuresOf(selected)) {
                val expH = expandedHeightAnimated(feature)
                if (my >= y && my <= y + rowHeight) {
                    onRowClick(feature, mx.toFloat(), contentLeft, rowWidth)
                    return true
                }
                y += rowHeight + expH + rowGap
            }
        }

        if (insideWindow(mx, my)) return true
        return super.mouseClicked(event, doubled)
    }

    private fun onRowClick(feature: Feature, mx: Float, x: Float, width: Float) {
        val toggleX = x + width - toggleWidth - 12f
        val onToggle = feature !== Interface && feature !== Connection && mx >= toggleX - 4f

        if (onToggle) {
            feature.toggle()
            Style.playClickSound(if (feature.enabled) 1.1f else 0.9f)
            return
        }

        if (feature.configSettings.isEmpty()) {
            feature.toggle()
            Style.playClickSound(if (feature.enabled) 1.1f else 0.9f)
            return
        }

        if (feature in expanded) expanded.remove(feature) else expanded.add(feature)
        Style.playClickSound(if (feature in expanded) 1.05f else 0.95f)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        FeatureManager.features.forEach { feature -> feature.configSettings.forEach { it.mouseReleased(event.button()) } }
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        val mx = Resolution.getMouseX(mouseX)
        val my = Resolution.getMouseY(mouseY)

        if (insideContent(mx, my)) {
            for (feature in featuresOf(selected)) {
                if (feature !in expanded) continue
                for (setting in visibleSettings(feature)) {
                    if (setting.mouseScrolled(mx, my, vertical)) return true
                }
            }
        }

        if (insideWindow(mx, my)) {
            scrollTarget = (scrollTarget - (vertical * 28).toFloat()).coerceIn(0f, maxScroll)
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        for (feature in expanded) {
            for (setting in visibleSettings(feature)) {
                if (setting.charTyped(event.codepoint.toChar())) return true
            }
        }
        return super.charTyped(event)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        for (feature in expanded) {
            for (setting in visibleSettings(feature)) {
                if (setting.keyPressed(event.key, event.scancode, event.modifiers)) return true
            }
        }

        if (event.key == InputConstants.KEY_ESCAPE && expanded.isNotEmpty()) {
            expanded.clear()
            return true
        }

        return super.keyPressed(event)
    }

    private fun insideContent(mx: Int, my: Int) =
        mx >= contentLeft && mx <= contentRight && my >= contentTop && my <= contentBottom

    private fun insideWindow(mx: Int, my: Int) =
        mx >= winX && mx <= winX + windowWidth && my >= winY && my <= winY + windowHeight

    override fun onClose() {
        expanded.clear()
        expandAnims.clear()
        needsOpenAnim = true
        Config.save()

        val parent = modMenuParent
        modMenuParent = null
        if (parent != null) {
            SkyblockConnect.mc.setScreenAndShow(parent)
            return
        }
        super.onClose()
    }

    override fun isPauseScreen() = false
}
