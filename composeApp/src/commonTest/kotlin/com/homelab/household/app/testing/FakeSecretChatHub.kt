package com.homelab.household.app.testing

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf

/**
 * The hub as a secret chat needs it: one secret chat, `s-1`, that answers 423 until the request
 * carries the unlock the PIN 482913 trades for. Any other PIN is refused with tries left.
 *
 * The chats list holds `s-1` back the same way: locked, with an empty title, until the unlock comes
 * with it. Its last message is ordinary, so its preview is sent either way.
 */
class FakeSecretChatHub {
    var unlockAttempts = 0
        private set

    /** False: the unlock call gets the proxy's 503, as when the hub is down. */
    var answering = true

    val engine: HttpClientEngine =
        MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path == "/api/v1/auth/unlock-secret" && !answering -> {
                    respond("", HttpStatusCode.ServiceUnavailable)
                }

                path == "/api/v1/auth/unlock-secret" && request.method == HttpMethod.Post -> {
                    unlockAttempts++
                    val body = (request.body as? TextContent)?.text.orEmpty()
                    if ("\"$RIGHT_PIN\"" in body) {
                        json("""{"unlock_token":"$UNLOCK","expires_in":600}""")
                    } else {
                        json("""{"detail":"Wrong PIN","attempts_left":4}""", HttpStatusCode.Forbidden)
                    }
                }

                path == "/api/v1/sessions/s-1" && request.method == HttpMethod.Get -> {
                    if (request.headers["X-Secret-Unlock"] == UNLOCK) {
                        json(SECRET_CHAT)
                    } else {
                        json("""{"detail":"locked","code":"secret_locked"}""", HttpStatusCode.Locked)
                    }
                }

                path == "/api/v1/sessions" && request.method == HttpMethod.Get -> {
                    json(if (request.headers["X-Secret-Unlock"] == UNLOCK) CHATS_UNLOCKED else CHATS_LOCKED)
                }

                path == "/api/v1/agents" -> {
                    json("[]")
                }

                path == "/api/v1/users/me" -> {
                    json(ME)
                }

                else -> {
                    json("{}")
                }
            }
        }

    private fun MockRequestHandleScope.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpResponseData = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    companion object {
        const val RIGHT_PIN = "482913"
        private const val UNLOCK = "unlock-1"
        const val SECRET_WORDS = "Plan the surprise for Saturday"
        const val ORDINARY_TITLE = "Groceries for the week"
        const val SECRET_TITLE = "Anniversary surprise"
        const val SECRET_PREVIEW = "Booked the table for eight"

        private val ME =
            """{"id":"emma","full_name":"Emma Larsson","avatar_color":"#3C6E4E","is_admin":true,
            "is_active":true,"created_at":"2026-09-16T00:00:00Z"}"""

        private const val ORDINARY_ROW = """{"id":"s-0","user_id":"emma","title":"$ORDINARY_TITLE"}"""

        private val CHATS_LOCKED =
            """[$ORDINARY_ROW,{"id":"s-1","user_id":"emma","title":"","is_secret":true,"has_secret_part":true,
            "locked":true,"last_message_preview":"$SECRET_PREVIEW"}]"""

        private val CHATS_UNLOCKED =
            """[$ORDINARY_ROW,{"id":"s-1","user_id":"emma","title":"$SECRET_TITLE","is_secret":true,
            "has_secret_part":true,"last_message_preview":"$SECRET_PREVIEW"}]"""

        private val SECRET_CHAT =
            """{"id":"s-1","user_id":"emma","title":"Saturday","is_secret":true,"has_secret_part":true,
            "messages":[{"id":"m-1","session_id":"s-1","role":"user","content":"$SECRET_WORDS","is_secret":true}]}"""
    }
}
