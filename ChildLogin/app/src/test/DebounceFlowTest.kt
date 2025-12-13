import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DebounceFlowTest {
    @Test
    fun debounce_300ms_emits_latest() = runTest(StandardTestDispatcher()) {
        val scope = TestScope()
        val input = MutableSharedFlow<CharSequence>(extraBufferCapacity = 10)
        val flow = input
            .debounce(300)
            .map { it.toString() }
            .distinctUntilChanged()
        flow.test {
            input.tryEmit("a")
            advanceTimeBy(100)
            input.tryEmit("ab")
            advanceTimeBy(250)
            input.tryEmit("abc")
            advanceTimeBy(300)
            assertEquals("ab", awaitItem())
            assertEquals("abc", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
