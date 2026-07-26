import com.skyblockconnect.features.impl.achievements.UltraRare
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UltraRareTest {

    @Test
    fun `matches the mythological rares`() {
        assertTrue(UltraRare.matches("Daedalus Axe"))
        assertTrue(UltraRare.matches("Crown of Greed"))
        assertTrue(UltraRare.matches("Enchanted Book (Chimera I)"))
    }

    @Test
    fun `matches slayer rares regardless of surrounding text`() {
        assertTrue(UltraRare.matches("Rare drop: Judgement Core (+50% Magic Find)"))
        assertTrue(UltraRare.matches("§6Wilson's Engineering Plans"))
        assertTrue(UltraRare.matches("Beheaded Horror"))
    }

    @Test
    fun `matches any dye`() {
        assertTrue(UltraRare.matches("Wild Strawberry Dye"))
        assertTrue(UltraRare.matches("Pure Black Dye"))
    }

    @Test
    fun `does not match a word that merely starts with a listed term`() {

        assertFalse(UltraRare.matches("Dyed Leather Chestplate"))
        assertFalse(UltraRare.matches("Chimerical Trinket"))
    }

    @Test
    fun `does not match ordinary drops`() {
        assertFalse(UltraRare.matches("Enchanted Book"))
        assertFalse(UltraRare.matches("Machine Gun Bow"))
        assertFalse(UltraRare.matches("Revenant Viscera"))
    }

    @Test
    fun `honours the user's extra terms`() {
        assertFalse(UltraRare.matches("Ice Sprayer"))
        assertTrue(UltraRare.matches("Ice Sprayer", extra = "Ice Sprayer, Glacite Amalgamation"))
        assertTrue(UltraRare.matches("Glacite Amalgamation", extra = " Ice Sprayer , Glacite Amalgamation "))
        assertFalse(UltraRare.matches("Ice Sprayer", extra = "  ,  "))
    }
}
