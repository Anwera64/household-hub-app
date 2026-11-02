package com.homelab.household.domain.secret

import com.homelab.household.domain.exception.SecretLockedException
import com.homelab.household.domain.repository.SecretUnlockRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Only the member's own input keeps secret chats open. Nothing here ever hears a stream event or a
 * poll, which is the point: an answer arriving is not the member being there.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SecretLockControllerTest {
    private class FakeUnlocks : SecretUnlockRepository {
        private val state = MutableStateFlow(false)
        override val holding: StateFlow<Boolean> = state
        override val locks: Flow<Unit> = emptyFlow()
        var age: Duration? = null
        var renewals = 0
        var renewRefuses = false

        override suspend fun unlock(pin: String) {
            state.value = true
            age = Duration.ZERO
        }

        override suspend fun renew() {
            renewals++
            if (renewRefuses) throw SecretLockedException()
            age = Duration.ZERO
        }

        override fun lock() {
            state.value = false
            age = null
        }

        override fun holdChatUnlock(
            sessionId: String,
            token: String,
        ) = Unit

        override fun tokenAge(): Duration? = age
    }

    private val unlocks = FakeUnlocks()
    private val signedOut = MutableSharedFlow<Unit>()

    private suspend fun TestScope.unlockedController(): SecretLockController {
        val controller = SecretLockController(unlocks, signedOut, backgroundScope)
        unlocks.unlock("1234")
        runCurrent()
        return controller
    }

    @Test
    fun `GIVEN unlocked WHEN ten minutes pass with no input THEN secret chats lock`() =
        runTest {
            unlockedController()

            advanceTimeBy(10.minutes - 1.seconds)
            assertTrue(unlocks.holding.value)
            advanceTimeBy(2.seconds)

            assertFalse(unlocks.holding.value)
        }

    @Test
    fun `GIVEN unlocked WHEN the member taps at nine minutes THEN it stays open until ten minutes after the tap`() =
        runTest {
            val controller = unlockedController()

            advanceTimeBy(9.minutes)
            controller.onUserInput()
            advanceTimeBy(6.minutes)
            assertTrue(unlocks.holding.value)
            advanceTimeBy(4.minutes + 1.seconds)

            assertFalse(unlocks.holding.value)
        }

    @Test
    fun `GIVEN unlocked WHEN the app stops THEN secret chats lock at once`() =
        runTest {
            val controller = unlockedController()

            controller.onAppStopped()

            assertFalse(unlocks.holding.value)
        }

    @Test
    fun `GIVEN unlocked WHEN the member is signed out THEN secret chats lock`() =
        runTest {
            unlockedController()

            signedOut.emit(Unit)
            runCurrent()

            assertFalse(unlocks.holding.value)
        }

    @Test
    fun `GIVEN an unlock two minutes old WHEN the member taps THEN it is renewed once`() =
        runTest {
            val controller = unlockedController()
            unlocks.age = 2.minutes

            controller.onUserInput()
            controller.onUserInput()
            runCurrent()

            assertEquals(1, unlocks.renewals)
        }

    @Test
    fun `GIVEN an unlock thirty seconds old WHEN the member taps THEN it is not renewed`() =
        runTest {
            val controller = unlockedController()
            unlocks.age = 30.seconds

            controller.onUserInput()
            runCurrent()

            assertEquals(0, unlocks.renewals)
        }

    @Test
    fun `GIVEN the hub no longer holds the unlock WHEN it is renewed THEN secret chats lock`() =
        runTest {
            val controller = unlockedController()
            unlocks.age = 2.minutes
            unlocks.renewRefuses = true

            controller.onUserInput()
            runCurrent()

            assertFalse(unlocks.holding.value)
        }

    @Test
    fun `GIVEN locked WHEN the member taps THEN nothing is renewed and nothing opens`() =
        runTest {
            val controller = SecretLockController(unlocks, signedOut, backgroundScope)
            unlocks.age = 2.minutes

            controller.onUserInput()
            runCurrent()

            assertEquals(0, unlocks.renewals)
            assertFalse(unlocks.holding.value)
        }
}
