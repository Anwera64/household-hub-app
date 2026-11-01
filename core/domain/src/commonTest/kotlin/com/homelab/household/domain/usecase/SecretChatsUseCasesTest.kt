package com.homelab.household.domain.usecase

import com.homelab.household.domain.repository.SecretUnlockRepository
import com.homelab.household.domain.usecase.impl.UnlockSecretChatsUseCaseImpl
import dev.mokkery.MockMode
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/** The PIN goes to the hub, which is the only thing that can say whether it opens secret chats. */
class SecretChatsUseCasesTest {
    private val unlocks = mock<SecretUnlockRepository>(MockMode.autoUnit)

    @Test
    fun `GIVEN a PIN WHEN secret chats are unlocked THEN the hub is asked with it`() =
        runTest {
            UnlockSecretChatsUseCaseImpl(unlocks)("482913")

            verifySuspend(VerifyMode.exactly(1)) { unlocks.unlock("482913") }
        }
}
