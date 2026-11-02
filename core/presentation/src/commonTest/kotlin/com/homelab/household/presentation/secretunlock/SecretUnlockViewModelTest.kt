package com.homelab.household.presentation.secretunlock

import app.cash.turbine.test
import com.homelab.household.domain.exception.PinLockedException
import com.homelab.household.domain.exception.ServerOfflineException
import com.homelab.household.domain.exception.WrongPinException
import com.homelab.household.domain.usecase.UnlockSecretChatsUseCase
import com.homelab.household.presentation.pinentry.PinStatus
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The locked chat's PIN pad: the sign-in pad's own behaviour, sending the PIN to unlock secret
 * chats instead of to sign in.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SecretUnlockViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val unlock = mock<UnlockSecretChatsUseCase>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun SecretUnlockViewModel.type(digits: String) = digits.forEach(::onDigit)

    @Test
    fun `GIVEN the right PIN WHEN the sixth digit is typed THEN secret chats unlock once`() =
        runTest(testDispatcher) {
            everySuspend { unlock(any()) } returns Unit
            val viewModel = SecretUnlockViewModel(unlock)

            viewModel.events.test {
                viewModel.type("482913")
                advanceUntilIdle()

                assertEquals(SecretUnlockEvent.Unlocked, awaitItem())
            }
            verifySuspend(VerifyMode.exactly(1)) { unlock("482913") }
        }

    @Test
    fun `GIVEN a wrong PIN WHEN it is typed THEN the dots clear and the tries left are kept`() =
        runTest(testDispatcher) {
            everySuspend { unlock(any()) } throws WrongPinException(attemptsLeft = 3)
            val viewModel = SecretUnlockViewModel(unlock)

            viewModel.type("000000")
            advanceUntilIdle()

            assertEquals(0, viewModel.uiState.value.entered)
            assertEquals(PinStatus.WrongPin(attemptsLeft = 3), viewModel.uiState.value.status)
        }

    @Test
    fun `GIVEN the tries used up WHEN a PIN is typed THEN it counts down and ignores the pad until then`() =
        runTest(testDispatcher) {
            everySuspend { unlock(any()) } throws PinLockedException(retryAfterSeconds = 30)
            val viewModel = SecretUnlockViewModel(unlock)

            viewModel.type("000000")
            advanceTimeBy(1)
            viewModel.type("1")

            assertEquals(PinStatus.Locked(secondsLeft = 30), viewModel.uiState.value.status)
            assertEquals(0, viewModel.uiState.value.entered)
            advanceTimeBy(30_001)
            assertEquals(PinStatus.Idle, viewModel.uiState.value.status)
        }

    @Test
    fun `GIVEN the hub out of reach WHEN a PIN is typed THEN it says so`() =
        runTest(testDispatcher) {
            everySuspend { unlock(any()) } throws ServerOfflineException("down")
            val viewModel = SecretUnlockViewModel(unlock)

            viewModel.type("482913")
            advanceUntilIdle()

            assertEquals(PinStatus.Unreachable, viewModel.uiState.value.status)
        }
}
