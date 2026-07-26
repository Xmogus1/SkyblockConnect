import com.skyblockconnect.features.impl.events.MiningEventParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MiningEventParserTest {

    @Test
    fun `reads name and timer off an active event bar`() {
        val parsed = MiningEventParser.parse("EVENT GOBLIN RAID ACTIVE IN Dwarven Mines for 17m 3s")
        assertEquals("Goblin Raid", parsed?.name)
        assertEquals(17 * 60 + 3, parsed?.secondsLeft)
    }

    @Test
    fun `handles the other verbs and hour-long timers`() {
        assertEquals("2x Powder", MiningEventParser.parse("EVENT 2X POWDER RUNNING IN Crystal Hollows for 1h 4m")?.name)
        assertEquals(3600 + 4 * 60, MiningEventParser.parse("EVENT 2X POWDER RUNNING IN Crystal Hollows for 1h 4m")?.secondsLeft)
        assertEquals(45, MiningEventParser.parse("EVENT RAFFLE STARTING IN Dwarven Mines for 45s")?.secondsLeft)
    }

    @Test
    fun `an event name it has never heard of still parses`() {

        val parsed = MiningEventParser.parse("EVENT SOME BRAND NEW THING ACTIVE IN Dwarven Mines for 5m")
        assertEquals("SOME BRAND NEW THING", parsed?.name)
    }

    @Test
    fun `a bar with no timer still yields the name`() {
        val parsed = MiningEventParser.parse("EVENT GOBLIN RAID ACTIVE IN Dwarven Mines")
        assertEquals("Goblin Raid", parsed?.name)
        assertNull(parsed?.secondsLeft)
    }

    @Test
    fun `finds a known event even when the wording is not the expected one`() {

        val parsed = MiningEventParser.parse("MITHRIL GOURMAND §7- 8m 12s")
        assertEquals("Mithril Gourmand", parsed?.name)
        assertEquals(8 * 60 + 12, parsed?.secondsLeft)
    }

    @Test
    fun `reports known events under one spelling whatever case Hypixel used`() {
        assertEquals("2x Powder", MiningEventParser.parse("EVENT 2X POWDER ACTIVE IN Dwarven Mines for 5m")?.name)
        assertEquals("Goblin Raid", MiningEventParser.parse("goblin raid 4m")?.name)
    }

    @Test
    fun `ignores bars that are not mining events`() {
        assertNull(MiningEventParser.parse("Wither King"))
        assertNull(MiningEventParser.parse("Voidgloom Seraph IV"))
        assertNull(MiningEventParser.parse(""))
        assertNull(MiningEventParser.parse("EVENT"))
    }

    @Test
    fun `duration parsing covers the formats Hypixel uses`() {
        assertEquals(74, MiningEventParser.parseDuration("1m14s"))
        assertEquals(74, MiningEventParser.parseDuration("1m 14s"))
        assertEquals(3600, MiningEventParser.parseDuration("1h"))
        assertNull(MiningEventParser.parseDuration("soon"))
    }

    @Test
    fun `formats durations for chat`() {
        assertEquals("45s", MiningEventParser.formatDuration(45))
        assertEquals("2m 5s", MiningEventParser.formatDuration(125))
        assertEquals("1h 5m", MiningEventParser.formatDuration(3900))
    }
}
