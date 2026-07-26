import com.skyblockconnect.features.impl.achievements.AchievementKind
import com.skyblockconnect.features.impl.achievements.AchievementScanner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AchievementScannerTest {

    @BeforeTest
    fun reset() = AchievementScanner.reset()

    private fun scan(raw: String, inDungeon: Boolean = true, now: Long = 1_000_000L) =
        AchievementScanner.scan(raw, inDungeon, now)

    @Test
    fun `a real chest payout offers only the items`() {

        assertNull(scan("  EMERALD CHEST REWARDS"))
        assertEquals(AchievementKind.DUNGEON_DROP to "Precursor Gear", scan("    Precursor Gear"))
        assertEquals(
            AchievementKind.DUNGEON_DROP to "Enchanted Book (Rejuvenate II)",
            scan("    Enchanted Book (Rejuvenate II)"),
        )
        assertEquals(AchievementKind.DUNGEON_DROP to "Power Dragon Shard", scan("    Power Dragon Shard"))
        assertNull(scan("    Undead Essence x90"))
        assertNull(scan("    Wither Essence x74"))
    }

    @Test
    fun `an unindented line closes the block`() {
        assertNull(scan("  OBSIDIAN CHEST REWARDS"))
        assertEquals(AchievementKind.DUNGEON_DROP to "Wither Shard", scan("    Wither Shard"))

        assertNull(scan("[SkyHanni] You have 358 items in stash. Click to pickup your stash!"))
        assertNull(scan("[SkyHanni] You have 59k materials in stash, totalling 13 types."))
    }

    @Test
    fun `a blank line closes the block`() {
        assertNull(scan("  EMERALD CHEST REWARDS"))
        assertEquals(AchievementKind.DUNGEON_DROP to "Precursor Gear", scan("    Precursor Gear"))
        assertNull(scan(""))

        assertNull(scan("    Some later indented line"))
    }

    @Test
    fun `never reacts to its own output`() {
        assertNull(scan("  EMERALD CHEST REWARDS"))
        assertNull(scan("SBC Shared."))
        assertNull(scan("SBC On cooldown - try again in 42s."))
    }

    @Test
    fun `the block cannot outlive its backstops`() {
        assertNull(scan("  EMERALD CHEST REWARDS", now = 0L))
        assertEquals(AchievementKind.DUNGEON_DROP to "Precursor Gear", scan("    Precursor Gear", now = 0L))

        assertNull(scan("    Precursor Gear", now = 30_000L))
    }

    @Test
    fun `rare rewards inside a chest still count`() {
        assertNull(scan("  OBSIDIAN CHEST REWARDS"))
        assertEquals(
            AchievementKind.DUNGEON_DROP to "RARE REWARD! Fuming Potato Book",
            scan("    RARE REWARD! Fuming Potato Book"),
        )
    }

    @Test
    fun `hoppity rabbit finds are detected for every rarity`() {

        assertEquals(
            AchievementKind.HOPPITY_RABBIT to "Divine Rabbit: Solomon",
            scan("HOPPITY'S HUNT You found Solomon (DIVINE)!", inDungeon = false),
        )
        assertEquals(
            AchievementKind.HOPPITY_RABBIT to "Common Rabbit: Arnie",
            scan("HOPPITY'S HUNT You found Arnie (COMMON)!", inDungeon = false),
        )

        assertEquals(
            AchievementKind.HOPPITY_RABBIT to "Mythic Rabbit: Sir Ryan",
            scan("HOPPITY'S HUNT You found Sir Ryan (MYTHIC)!", inDungeon = false),
        )
    }

    @Test
    fun `skyblock level ups are detected`() {
        assertEquals(
            AchievementKind.SKYBLOCK_LEVEL to "SkyBlock Level 525",
            scan("Level 524 ➡ [525]", inDungeon = false),
        )
        assertEquals(
            AchievementKind.SKYBLOCK_LEVEL to "SkyBlock Level 100",
            scan("SkyBlock Level 99 ➜ [100]", inDungeon = false),
        )
    }

    @Test
    fun `standalone drops are unaffected`() {
        assertEquals(
            AchievementKind.RARE_DROP to "Potato (+156 Magic Find)",
            scan("RARE DROP! Potato (+156 Magic Find)", inDungeon = false),
        )
        assertEquals(
            AchievementKind.DUNGEON_DROP to "Recombobulator 3000",
            scan("RARE DROP! Recombobulator 3000", inDungeon = true),
        )
        assertEquals(
            AchievementKind.SKILL_LEVEL to "Combat 24 ➜ 25",
            scan("SKILL LEVEL UP Combat 24 ➜ 25", inDungeon = false),
        )
    }

    @Test
    fun `ordinary chatter is ignored`() {
        assertNull(scan("Friend > Strikeninja010 left.", inDungeon = false))
        assertNull(scan("Warping...", inDungeon = false))
        assertNull(scan("§7==== Restored Messages ====§r", inDungeon = false))
    }
}
