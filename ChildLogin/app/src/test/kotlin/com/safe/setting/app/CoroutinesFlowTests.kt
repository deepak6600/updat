package com.safe.setting.app

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutinesFlowTests {

    @Test
    fun debounce_distinct_300ms() = runTest {
        val events = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 64)
        val debounced: Flow<Int> = events
            .debounce(300)
            .distinctUntilChanged()
        debounced.test {
            events.emit(1)
            events.emit(1)
            events.emit(2)
            // advance virtual time by 300ms
            delay(300)
            assertEquals(2, awaitItem())
            events.emit(2)
            events.emit(3)
            delay(300)
            assertEquals(3, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun retry_exponential_backoff_with_jitter() = runTest {
        var attempts = 0
        val baseDelayMs = 500L
        val multiplier = 2.0
        val maxDelayMs = 10_000L
        val jitterPct = 0.2

        val failingFlow = flow {
            attempts++
            if (attempts <= 3) throw RuntimeException("fail $attempts")
            emit("ok")
        }.retryWhen { cause, attempt ->
            if (attempt < 3) {
                val computed = (baseDelayMs * Math.pow(multiplier, attempt.toDouble())).toLong()
                val delayMs = min(maxDelayMs, computed)
                val jitter = (delayMs * jitterPct).toLong()
                val jittered = delayMs + Random.nextLong(-jitter, jitter)
                delay(max(0L, jittered))
                true
            } else false
        }

        failingFlow.test {
            // expect single success after 3 failures
            assertEquals("ok", awaitItem())
            awaitComplete()
        }
    }
}
