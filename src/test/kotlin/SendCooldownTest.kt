import com.skyblockconnect.features.impl.achievements.SendCooldown
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SendCooldownTest {

    @BeforeTest
    fun reset() = SendCooldown.reset()

    @Test
    fun `the first send goes through`() {
        assertTrue(SendCooldown.ready)
        assertTrue(SendCooldown.tryConsume())
    }

    @Test
    fun `a second send within the minute is blocked`() {
        val now = 1_000_000L
        assertTrue(SendCooldown.tryConsume(now))
        assertFalse(SendCooldown.tryConsume(now + 1))
        assertFalse(SendCooldown.tryConsume(now + 59_000L))
    }

    @Test
    fun `sending is allowed again once the minute is up`() {
        val now = 1_000_000L
        assertTrue(SendCooldown.tryConsume(now))
        assertTrue(SendCooldown.tryConsume(now + SendCooldown.COOLDOWN_MS))
    }

    @Test
    fun `a blocked attempt does not push the cooldown back`() {

        val now = 1_000_000L
        assertTrue(SendCooldown.tryConsume(now))
        repeat(5) { assertFalse(SendCooldown.tryConsume(now + 10_000L)) }
        assertTrue(SendCooldown.tryConsume(now + SendCooldown.COOLDOWN_MS))
    }

    @Test
    fun `remaining time counts down`() {
        val now = 1_000_000L
        SendCooldown.tryConsume(now)
        assertEquals(SendCooldown.COOLDOWN_MS, SendCooldown.remainingMs(now))
        assertEquals(30_000L, SendCooldown.remainingMs(now + 30_000L))
        assertEquals(0L, SendCooldown.remainingMs(now + 60_000L))
    }
}
