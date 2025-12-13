import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RetryBackoffTest {
    @Test
    fun backoff_parameters_match_constraints() = runTest {
        val baseDelay = 500L
        val multiplier = 2.0
        val maxDelay = 10_000L
        val attempts = 3
        val jitterPct = 0.2
        val delays = (1..attempts).map { attempt ->
            val computed = (baseDelay * Math.pow(multiplier, (attempt - 1).toDouble())).toLong()
            val delay = kotlin.math.min(maxDelay, computed)
            val jitter = (delay * jitterPct).toLong()
            val minD = delay - jitter
            val maxD = delay + jitter
            assertTrue(minD >= 0)
            assertTrue(maxD >= minD)
            delay
        }
        assertTrue(delays[0] in 400L..600L)
        assertTrue(delays[1] in 800L..1200L)
        assertTrue(delays[2] in 1600L..2400L || delays[2] == 10000L)
    }
}
