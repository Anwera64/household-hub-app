package com.homelab.household.data.datasource.remote

import com.homelab.household.data.di.DEFAULT_BASE_URL
import com.homelab.household.data.dto.SecretUnlockReadDto
import com.homelab.household.domain.exception.PinLockedException
import com.homelab.household.domain.exception.WrongPinException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The PIN opens every secret chat on this phone. It is refused the way any signed-in PIN check is:
 * 403 with the tries left, then 429 with the wait — never 401, which would sign the member out.
 */
class KtorSecretUnlockRemoteDataSourceTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun dataSource(
        status: HttpStatusCode,
        body: String,
        seen: MutableList<Pair<String, String?>> = mutableListOf(),
    ) = KtorSecretUnlockRemoteDataSource(
        client =
            HttpClient(
                MockEngine { request ->
                    seen += "${request.method.value} ${request.url.encodedPath}" to (request.body as? TextContent)?.text
                    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                },
            ) { install(ContentNegotiation) { json(json) } },
        baseUrl = DEFAULT_BASE_URL,
    )

    @Test
    fun `GIVEN the right PIN WHEN secret chats are unlocked THEN the PIN is posted and the unlock comes back`() =
        runTest {
            val seen = mutableListOf<Pair<String, String?>>()

            val unlock =
                dataSource(HttpStatusCode.OK, """{"unlock_token":"unlock-1","expires_in":600}""", seen).unlock("1234")

            assertEquals(SecretUnlockReadDto(unlock_token = "unlock-1", expires_in = 600), unlock)
            assertEquals("${HttpMethod.Post.value} /api/v1/auth/unlock-secret", seen.single().first)
            assertEquals(Json.parseToJsonElement("""{"pin":"1234"}"""), Json.parseToJsonElement(seen.single().second!!))
        }

    @Test
    fun `GIVEN a wrong PIN WHEN secret chats are unlocked THEN it says how many tries are left`() =
        runTest {
            val refusal =
                assertFailsWith<WrongPinException> {
                    dataSource(HttpStatusCode.Forbidden, """{"detail":"Wrong PIN","attempts_left":2}""").unlock("0000")
                }

            assertEquals(2, refusal.attemptsLeft)
        }

    @Test
    fun `GIVEN the tries are used up WHEN secret chats are unlocked THEN it says how long to wait`() =
        runTest {
            val refusal =
                assertFailsWith<PinLockedException> {
                    dataSource(HttpStatusCode.TooManyRequests, """{"detail":"Locked","retry_after_seconds":300}""")
                        .unlock("0000")
                }

            assertEquals(300, refusal.retryAfterSeconds)
        }

    @Test
    fun `GIVEN an unlock held WHEN it is renewed THEN the renewal is posted and a fresh unlock comes back`() =
        runTest {
            val seen = mutableListOf<Pair<String, String?>>()

            val unlock = dataSource(HttpStatusCode.OK, """{"unlock_token":"unlock-2","expires_in":600}""", seen).renew()

            assertEquals("unlock-2", unlock.unlock_token)
            assertEquals("${HttpMethod.Post.value} /api/v1/auth/unlock-secret/renew", seen.single().first)
        }
}
