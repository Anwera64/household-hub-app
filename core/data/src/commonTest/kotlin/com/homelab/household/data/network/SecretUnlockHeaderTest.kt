package com.homelab.household.data.network

import com.homelab.household.data.datasource.local.InMemorySessionStorage
import com.homelab.household.data.datasource.local.SecretUnlockLocalDataSource
import com.homelab.household.domain.exception.SecretLockedException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * The unlock goes only where the hub reads it: the chats and the renewal. A 423 is a lock, never a
 * sign-out, so the member's own token is left alone while the refused unlock is forgotten.
 */
class SecretUnlockHeaderTest {
    private val hub = "https://hub.test.local"
    private val sent = mutableListOf<String?>()

    private fun client(
        unlocks: SecretUnlockLocalDataSource,
        status: HttpStatusCode = HttpStatusCode.OK,
        storage: InMemorySessionStorage = InMemorySessionStorage(),
    ) = HttpClient(
        MockEngine { request ->
            sent += request.headers[SECRET_UNLOCK_HEADER]
            val body = if (status == HttpStatusCode.Locked) """{"detail":"locked","code":"secret_locked"}""" else "[]"
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        },
    ) {
        sendSecretUnlock(unlocks)
        signOutOnUnauthorized(storage) {}
    }

    @Test
    fun `GIVEN secret chats unlocked WHEN the chats are listed or one is opened THEN the unlock goes with each call`() =
        runTest {
            val unlocks = SecretUnlockLocalDataSource().apply { hold("unlock-1") }

            client(unlocks).get("$hub/api/v1/sessions")
            client(unlocks).get("$hub/api/v1/sessions/chat-1")

            assertEquals(listOf<String?>("unlock-1", "unlock-1"), sent)
        }

    @Test
    fun `GIVEN secret chats unlocked WHEN any other call is made THEN the unlock stays on the phone`() =
        runTest {
            val unlocks = SecretUnlockLocalDataSource().apply { hold("unlock-1") }

            client(unlocks).get("$hub/api/v1/spaces/shared")
            client(unlocks).get("$hub/api/v1/memories")

            assertEquals(listOf<String?>(null, null), sent)
        }

    @Test
    fun `GIVEN secret chats locked again WHEN a chat is opened THEN no unlock is sent`() =
        runTest {
            val unlocks =
                SecretUnlockLocalDataSource().apply {
                    hold("unlock-1")
                    forgetAll()
                }

            client(unlocks).get("$hub/api/v1/sessions/chat-1")

            assertEquals(listOf<String?>(null), sent)
        }

    @Test
    fun `GIVEN only one chat's own unlock WHEN that chat and another are opened THEN only that chat carries it`() =
        runTest {
            val unlocks = SecretUnlockLocalDataSource().apply { holdChat("chat-1", "chat-unlock") }

            client(unlocks).post("$hub/api/v1/sessions/chat-1/chat/stream")
            client(unlocks).get("$hub/api/v1/sessions/chat-2")

            assertEquals(listOf("chat-unlock", null), sent)
        }

    @Test
    fun `GIVEN the hub no longer holds the unlock WHEN a chat answers 423 THEN it reads as locked and the unlock is forgotten`() =
        runTest {
            val unlocks = SecretUnlockLocalDataSource().apply { hold("expired-unlock") }

            assertFailsWith<SecretLockedException> {
                client(unlocks, HttpStatusCode.Locked).get("$hub/api/v1/sessions/chat-1")
            }

            assertFalse(unlocks.holding.value)
            assertNull(unlocks.tokenFor("chat-1"))
        }

    @Test
    fun `GIVEN a member signed in WHEN a chat answers 423 THEN they stay signed in`() =
        runTest {
            val storage = InMemorySessionStorage().apply { saveTokens("member-token") }

            assertFailsWith<SecretLockedException> {
                client(SecretUnlockLocalDataSource(), HttpStatusCode.Locked, storage).get("$hub/api/v1/sessions/chat-1")
            }

            assertEquals("member-token", storage.getAccessToken())
        }
}
