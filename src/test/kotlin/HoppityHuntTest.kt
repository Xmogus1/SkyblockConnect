import com.skyblockconnect.features.impl.achievements.HoppityHunt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HoppityHuntTest {

    @Test
    fun `reads the rarity off the scanner payload`() {
        assertEquals("DIVINE", HoppityHunt.rarityOf("Divine Rabbit: Solomon"))
        assertEquals("MYTHIC", HoppityHunt.rarityOf("Mythic Rabbit: Sir Ryan"))
        assertEquals("COMMON", HoppityHunt.rarityOf("Common Rabbit: Arnie"))
    }

    @Test
    fun `is null for anything that is not a rabbit payload`() {
        assertNull(HoppityHunt.rarityOf("Rare drop: Judgement Core"))
        assertNull(HoppityHunt.rarityOf("Solomon"))
        assertNull(HoppityHunt.rarityOf(""))
    }

    @Test
    fun `reads the rarity out of the decorated, formatted payload`() {

        assertEquals("DIVINE", HoppityHunt.rarityOf("§dHoppity's Hunt§7: §rDivine Rabbit: Solomon"))
        assertEquals("MYTHIC", HoppityHunt.rarityOf("§dHoppity's Hunt§7: §r§dMythic Rabbit§7: §fSigrid"))
    }

    @Test
    fun `colours the body by SkyBlock rarity`() {
        assertEquals("§b", HoppityHunt.rarityColor("DIVINE"))
        assertEquals("§d", HoppityHunt.rarityColor("MYTHIC"))
        assertEquals("§6", HoppityHunt.rarityColor("legendary"))
        assertEquals("§bDivine Rabbit§7: §fSolomon", HoppityHunt.styleBody("Divine Rabbit: Solomon"))
        assertEquals("§dMythic Rabbit§7: §fSir Ryan", HoppityHunt.styleBody("Mythic Rabbit: Sir Ryan"))
    }
}
