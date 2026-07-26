package com.skyblockconnect.features.impl.events

import com.skyblockconnect.event.impl.TickEvent
import com.skyblockconnect.event.impl.WorldChangeEvent
import com.skyblockconnect.features.Feature
import com.skyblockconnect.features.impl.general.Connection
import com.skyblockconnect.ui.clickgui.components.impl.SliderSetting
import com.skyblockconnect.ui.clickgui.components.impl.TextInputSetting
import com.skyblockconnect.ui.clickgui.components.impl.ToggleSetting
import com.skyblockconnect.utils.Announce
import com.skyblockconnect.utils.BossBarUtils
import com.skyblockconnect.utils.ScoreboardUtils
import com.skyblockconnect.utils.location.LocationUtils
import com.skyblockconnect.utils.location.WorldType
import com.skyblockconnect.utils.render.Render2D
import com.skyblockconnect.utils.render.Render2D.width
import com.skyblockconnect.websocket.RecentFeed
import com.skyblockconnect.websocket.SbcNet
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents
import java.awt.Color

object MiningEvents: Feature("Share and receive the mining event running in Dwarven Mines / Crystal Hollows.", toggled = true) {

    val share by ToggleSetting("Share My Lobby", true)
        .withDescription("Report the event running in your lobby to everyone else.")

    val receive by ToggleSetting("Announce In Chat", true)
        .withDescription("Print mining events other players report. The HUD works either way.")

    private val playSound by ToggleSetting("Play Sound", false)
        .withDescription("Play a soft ping when an event arrives.")

    private val filter by TextInputSetting("Only Show", "")
        .withDescription("Comma-separated words; only events containing one of them are announced. Empty = announce all.")

    private val minMinutesLeft by SliderSetting("Hide Under (min)", 0, 0, 20, 1)
        .withDescription("Skip announcements whose event has less than this many minutes left.")

    private val reshareMinutes by SliderSetting("Re-share Every (min)", 5, 1, 20, 1)
        .withDescription("How often the same ongoing event is reported again.")

    private val showHud by ToggleSetting("Show HUD", true).section("HUD")
        .withDescription("Draw the live mining events on screen. Move it with /sbc hud.")

    private val hudRows by SliderSetting("Max Rows", 5, 1, 12, 1)
        .withDescription("How many lobbies to list at once.")
        .showIf { showHud.value }

    private val hudShowOwn by ToggleSetting("Include My Lobby", true)
        .withDescription("List the event running where you are standing, marked as yours.")
        .showIf { showHud.value }

    private val hudOnlyInMining by ToggleSetting("Only On Mining Islands", true)
        .withDescription("Hide the HUD unless you are in the Dwarven Mines, Crystal Hollows or a Mineshaft.")
        .showIf { showHud.value }

    private val miningIslands = setOf(WorldType.DwarvenMines, WorldType.CrystalHollows, WorldType.Mineshaft)

    private var lastKey = ""
    private var lastShareAt = 0L
    private var tickCounter = 0

    private val headerColor = Color(255, 187, 61)
    private val textColor = Color(238, 236, 248)
    private val mutedColor = Color(150, 145, 175)
    private val oddOneOutColor = Color(255, 120, 110)

    override fun init() {
        register<TickEvent.Start> {

            if (++ tickCounter % 20 != 0) return@register
            if (! LocationUtils.inSkyblock || LocationUtils.world !in miningIslands) return@register

            val (event, endsIn) = readEvent() ?: return@register
            val island = LocationUtils.world?.tabName ?: return@register
            val server = LocationUtils.serverId

            MiningEventBoard.put(island, server, event, endsIn, player = null, mine = true)

            if (! share.value) return@register

            val key = "$island|$event|$server"
            val now = System.currentTimeMillis()
            val stale = now - lastShareAt > reshareMinutes.value * 60_000L
            if (key == lastKey && ! stale) return@register

            lastKey = key
            lastShareAt = now
            SbcNet.sendMiningEvent(island, event, server, endsIn)
        }

        register<WorldChangeEvent> {
            lastKey = ""
            tickCounter = 0
        }

        hudElement("Mining Events", enabled = { showHud.value }, shouldDraw = { hudVisible() }) { ctx, example ->
            drawHud(ctx, example)
        }
    }

    fun readEvent(): Pair<String, Int?>? {
        BossBarUtils.lines().forEach { bar ->
            MiningEventParser.parse(bar)?.let { return it.name to it.secondsLeft }
        }

        val name = ScoreboardUtils.valueOf("Event:") ?: ScoreboardUtils.valueOf("Next Event:") ?: return null
        val timer = ScoreboardUtils.valueOf("Ends In:") ?: ScoreboardUtils.valueOf("Starts In:")
        return name to timer?.let(MiningEventParser::parseDuration)
    }

    fun onRemoteEvent(island: String, event: String, server: String?, endsIn: Int?, player: String?, fromSnapshot: Boolean, historyOnly: Boolean = false) {
        if (! enabled) return

        val body = "§6§lMINING §r§e$event §7in §f$island" + (server?.let { " §8($it)" } ?: "")
        if (historyOnly) { RecentFeed.record("Events", player, body); return }

        MiningEventBoard.put(island, server, event, endsIn, player, mine = false)

        val now = System.currentTimeMillis()
        val dedupKey = "$island|$event"
        if (now < announcedUntil.getOrDefault(dedupKey, 0L)) return
        announcedUntil[dedupKey] = now + (endsIn?.toLong()?.times(1000L) ?: 300_000L).coerceIn(60_000L, 1_200_000L)

        RecentFeed.record("Events", player, body)

        if (! receive.value) return
        if (Connection.quietWhenOffSkyblock.value && ! LocationUtils.inSkyblock) return

        val wanted = filter.value.split(',').map(String::trim).filter(String::isNotEmpty)
        if (wanted.isNotEmpty() && wanted.none { event.contains(it, ignoreCase = true) }) return
        if (endsIn != null && endsIn < minMinutesLeft.value * 60) return

        Announce.chat(body, sound = if (playSound.value) SoundEvents.NOTE_BLOCK_PLING.value() else null)
    }

    private val announcedUntil = HashMap<String, Long>()

    private fun hudVisible(): Boolean {
        if (! showHud.value) return false
        if (hudOnlyInMining.value && LocationUtils.world !in miningIslands) return false
        return MiningEventBoard.live().isNotEmpty()
    }

    private fun drawHud(ctx: GuiGraphicsExtractor, example: Boolean): Pair<Float, Float> {
        val now = System.currentTimeMillis()
        val rows = if (example) exampleRows() else buildRows(now)
        if (rows.isEmpty()) return 0f to 0f

        val lineHeight = 10f
        var y = 0f

        val header = "§6§l⛏ Mining Events"
        Render2D.drawString(ctx, header, 0f, y, headerColor)
        y += lineHeight + 2f

        var maxWidth = header.width().toFloat()

        rows.forEach { row ->
            Render2D.drawString(ctx, row.text, 0f, y, row.color)
            maxWidth = maxOf(maxWidth, row.text.width().toFloat())
            y += lineHeight
        }

        return maxWidth to y
    }

    private data class Row(val text: String, val color: Color)

    private fun buildRows(now: Long): List<Row> {
        val live = MiningEventBoard.live(now).filter { hudShowOwn.value || ! it.mine }
        if (live.isEmpty()) return emptyList()

        val baseline = MiningEventBoard.baseline(now)
        val rows = live.take(hudRows.value).map { entry ->
            val differs = baseline != null && entry.event != baseline
            val who = when {
                entry.mine -> "§8(you)"
                entry.player != null -> "§8${entry.player}"
                else -> ""
            }
            val left = entry.secondsLeft(now)?.let { " §7${MiningEventParser.formatDuration(it)}" }.orEmpty()
            val mark = if (differs) " §c≠" else ""

            Row(
                "§e${entry.event} §7${entry.server ?: entry.island} $who$left$mark",
                if (differs) oddOneOutColor else textColor,
            )
        }.toMutableList()

        val distinct = MiningEventBoard.distinctEvents(now)
        if (distinct > 1) {
            val ownEvent = MiningEventBoard.own(now)?.event
            rows += Row(
                if (ownEvent != null) "§c≠ §7others are running a different event"
                else "§c≠ §7$distinct different events live",
                mutedColor,
            )
        }

        val hidden = live.size - hudRows.value
        if (hidden > 0) rows += Row("§8+$hidden more", mutedColor)

        return rows
    }

    private fun exampleRows() = listOf(
        Row("§e2x Powder §7mini1A §8(you) §712m 4s", textColor),
        Row("§eMithril Gourmand §7mini7B §8Steve §73m 20s §c≠", oddOneOutColor),
        Row("§c≠ §7others are running a different event", mutedColor),
    )
}