package com.homelab.household.data.repository

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

            assertTrue(repository.unlocked.value)
            assertEquals("unlock-1", local.tokenFor(null))
            assertEquals(0.minutes, repository.tokenAge())
        }

    @Test
    fun `GIVEN an unlock two minutes old WHEN it is renewed THEN the fresh one replaces it and its age starts again`() =
        runTest {
            everySuspend { remote.unlock("1234") } returns SecretUnlockReadDto("unlock-1", 600)
            everySuspend { remote.renew() } returns SecretUnlockReadDto("unlock-2", 600)
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

            assertFalse(repository.unlocked.value)
            assertNull(local.tokenFor("chat-1"))
            assertNull(repository.tokenAge())
        }

    @Test
    fun `GIVEN only one chat's own unlock WHEN asked whether secret chats are open THEN they are not`() {
        repository.holdChatUnlock("chat-1", "chat-unlock")

        assertFalse(repository.unlocked.value)
        assertNull(repository.tokenAge())
    }
}
