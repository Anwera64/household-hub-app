package com.homelab.household.domain.usecase

import com.homelab.household.domain.model.User
import com.homelab.household.domain.repository.AuthRepository
import com.homelab.household.domain.repository.SecretPreferencesRepository
import com.homelab.household.domain.usecase.impl.AsksBeforeSecretOffUseCaseImpl
import com.homelab.household.domain.usecase.impl.StopAskingBeforeSecretOffUseCaseImpl
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** "Don't ask again" on the turn-off sheet belongs to whoever is signed in on this phone. */
class SecretOffPreferenceUseCasesTest {
    private val auth = mock<AuthRepository>()
    private val preferences = mock<SecretPreferencesRepository>(MockMode.autoUnit)

    private fun signedIn(user: User?) = everySuspend { auth.getCurrentUser() } returns user

    @Test
    fun `GIVEN Emma never ticked Don't ask again WHEN she turns Secret Mode off THEN the phone asks first`() =
        runTest {
            signedIn(EMMA)
            every { preferences.asksBeforeTurningOff("emma") } returns true

            assertTrue(AsksBeforeSecretOffUseCaseImpl(auth, preferences)())
        }

    @Test
    fun `GIVEN Emma ticked Don't ask again WHEN she turns Secret Mode off THEN the phone does not ask`() =
        runTest {
            signedIn(EMMA)
            every { preferences.asksBeforeTurningOff("emma") } returns false

            assertFalse(AsksBeforeSecretOffUseCaseImpl(auth, preferences)())
        }

    @Test
    fun `GIVEN the phone cannot tell who is signed in WHEN Secret Mode is turned off THEN it asks first`() =
        runTest {
            signedIn(null)

            assertTrue(AsksBeforeSecretOffUseCaseImpl(auth, preferences)())
        }

    @Test
    fun `GIVEN Emma WHEN she ticks Don't ask again THEN it is kept for her`() =
        runTest {
            signedIn(EMMA)

            StopAskingBeforeSecretOffUseCaseImpl(auth, preferences)()

            verify(VerifyMode.exactly(1)) { preferences.stopAskingBeforeTurningOff("emma") }
        }

    @Test
    fun `GIVEN the phone cannot tell who is signed in WHEN Don't ask again is ticked THEN nothing is kept`() =
        runTest {
            signedIn(null)

            StopAskingBeforeSecretOffUseCaseImpl(auth, preferences)()

            verify(VerifyMode.not) { preferences.stopAskingBeforeTurningOff(any()) }
        }

    private companion object {
        val EMMA = User(id = "emma", fullName = "Emma Larsson", isAdmin = true, isActive = true)
    }
}
