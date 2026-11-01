package com.homelab.household.data.repository

import app.cash.turbine.test
import com.homelab.household.data.datasource.local.SecretUnlockLocalDataSource
import com.homelab.household.data.datasource.remote.`interface`.SecretUnlockRemoteDataSource
import com.homelab.household.data.dto.SecretUnlockReadDto
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TestTimeSource

/** The unlock lives in memory only, and its age is what decides when the member's taps renew it. */
class SecretUnlockRepositoryTest {
    private val clock = TestTimeSource()
    private val local = SecretUnlockLocalDataSource(clock)
    private val remote = mock<SecretUnlockRemoteDataSource>()
    private val repository = SecretUnlockRepositoryImpl(remote, local)

    @Test
    fun `GIVEN the right PIN WHEN secret chats are unlocked THEN they open and the unlock is new`() =
        runTest {
            everySuspend { remote.unlock("1234") } returns SecretUnlockReadDto("unlock-1", 600)

            repository.unlock("1234")

            assertTrue(repository.holding.value)
            assertEquals("unlock-1", local.tokenFor(null))
            assertEquals(0.minutes, repository.tokenAge())
        }

    @Test
    fun `GIVEN an unlock two minutes old WHEN it is renewed THEN the fresh one replaces it and its age starts again`() =
        runTest {
            everySuspend { remote.unlock("1234") } returns SecretUnlockReadDto("unlock-1", 600)
            everySuspend { remote.renew("unlock-1") } returns SecretUnlockReadDto("unlock-2", 600)
            repository.unlock("1234")
            clock += 2.minutes
            assertEquals(2.minutes, repository.tokenAge())

            repository.renew()

            assertEquals("unlock-2", local.tokenFor(null))
            assertEquals(0.minutes, repository.tokenAge())
        }

    @Test
    fun `GIVEN secret chats and one chat unlocked WHEN they lock THEN every unlock is forgotten`() =
        runTest {
            everySuspend { remote.unlock("1234") } returns SecretUnlockReadDto("unlock-1", 600)
            repository.unlock("1234")
            repository.holdChatUnlock("chat-1", "chat-unlock")

            repository.lock()

            assertFalse(repository.holding.value)
            assertNull(local.tokenFor("chat-1"))
            assertNull(repository.tokenAge())
        }

    @Test
    fun `GIVEN secret chats unlocked WHEN they lock THEN whoever is showing one hears it`() =
        runTest {
            everySuspend { remote.unlock("1234") } returns SecretUnlockReadDto("unlock-1", 600)
            repository.unlock("1234")

            repository.locks.test {
                repository.lock()

                assertEquals(Unit, awaitItem())
            }
        }

    @Test
    fun `GIVEN only one chat's own unlock WHEN asked what is held THEN it is held and ages`() {
        repository.holdChatUnlock("chat-1", "chat-unlock")
        clock += 2.minutes

        assertTrue(repository.holding.value)
        assertEquals(2.minutes, repository.tokenAge())
    }

    @Test
    fun `GIVEN only one chat's own unlock WHEN it is renewed THEN that chat carries the fresh one`() =
        runTest {
            everySuspend { remote.renew("chat-unlock") } returns SecretUnlockReadDto("chat-unlock-2", 600)
            repository.holdChatUnlock("chat-1", "chat-unlock")
            clock += 2.minutes

            repository.renew()

            assertEquals("chat-unlock-2", local.tokenFor("chat-1"))
            assertEquals(0.minutes, repository.tokenAge())
        }
}
