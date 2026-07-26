import com.skyblockconnect.features.impl.events.MiningEventBoard
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MiningEventBoardTest {

    @BeforeTest
    fun reset() = MiningEventBoard.clear()

    @Test
    fun `a later report about the same lobby replaces the earlier one`() {
        MiningEventBoard.put("Dwarven Mines", "mini1A", "2x Powder", 600, "Steve", mine = false)
        MiningEventBoard.put("Dwarven Mines", "mini1A", "Goblin Raid", 300, "Alex", mine = false)

        val live = MiningEventBoard.live()
        assertEquals(1, live.size)
        assertEquals("Goblin Raid", live.single().event)
    }

    @Test
    fun `different lobbies are tracked separately`() {
        MiningEventBoard.put("Dwarven Mines", "mini1A", "2x Powder", 600, "Steve", mine = false)
        MiningEventBoard.put("Dwarven Mines", "mini7B", "2x Powder", 600, "Alex", mine = false)
        assertEquals(2, MiningEventBoard.live().size)
    }

    @Test
    fun `your own lobby sorts first`() {
        MiningEventBoard.put("Dwarven Mines", "mini1A", "2x Powder", 900, "Steve", mine = false)
        MiningEventBoard.put("Dwarven Mines", "mini7B", "Raffle", 60, null, mine = true)
        assertTrue(MiningEventBoard.live().first().mine)
    }

    @Test
    fun `expired events drop off`() {
        val now = System.currentTimeMillis()
        MiningEventBoard.put("Dwarven Mines", "mini1A", "2x Powder", 60, "Steve", mine = false)
        assertEquals(1, MiningEventBoard.live(now).size)

        assertEquals(0, MiningEventBoard.live(now + 61_000L).size)
    }

    @Test
    fun `an event with no timer survives on the fallback lifetime`() {
        val now = System.currentTimeMillis()
        MiningEventBoard.put("Dwarven Mines", "mini1A", "Raffle", null, "Steve", mine = false)
        assertEquals(1, MiningEventBoard.live(now + 60_000L).size)
        assertEquals(0, MiningEventBoard.live(now + 11 * 60_000L).size)
    }

    @Test
    fun `baseline is your own event when SBC knows it`() {
        MiningEventBoard.put("Dwarven Mines", "mini1A", "2x Powder", 600, "Steve", mine = false)
        MiningEventBoard.put("Dwarven Mines", "mini2A", "2x Powder", 600, "Alex", mine = false)
        MiningEventBoard.put("Dwarven Mines", "mini7B", "Mithril Gourmand", 600, null, mine = true)

        assertEquals("Mithril Gourmand", MiningEventBoard.baseline())
        assertEquals(2, MiningEventBoard.distinctEvents())
    }

    @Test
    fun `baseline falls back to the most common event`() {
        MiningEventBoard.put("Dwarven Mines", "mini1A", "2x Powder", 600, "Steve", mine = false)
        MiningEventBoard.put("Dwarven Mines", "mini2A", "2x Powder", 600, "Alex", mine = false)
        MiningEventBoard.put("Dwarven Mines", "mini7B", "Goblin Raid", 600, "Herobrine", mine = false)

        assertEquals("2x Powder", MiningEventBoard.baseline())
    }

    @Test
    fun `an empty board has no baseline`() {
        assertNull(MiningEventBoard.baseline())
        assertEquals(0, MiningEventBoard.distinctEvents())
    }
}
