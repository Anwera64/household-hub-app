package com.homelab.household.app.testing

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/**
 * The hub as the privacy offer needs it: one ordinary chat, `s-2`, whose every new message the hub
 * judges private. Its answer says the turn was kept private and suggests Secret Mode; "Not now" is
 * counted, and turning Secret Mode on hands back the chat's own unlock.
 */
class FakePrivacyHub {
    var declines = 0
        private set

    val engine: HttpClientEngine =
        MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path == "/api/v1/sessions/s-2" && request.method == HttpMethod.Get -> {
                    json(ORDINARY_CHAT)
                }

                path == "/api/v1/sessions/s-2/chat/stream" && request.method == HttpMethod.Post -> {
                    respond(PRIVATE_TURN, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/event-stream"))
                }

                path == "/api/v1/sessions/s-2/secret-offer/decline" && request.method == HttpMethod.Post -> {
                    declines++
                    respond("", HttpStatusCode.NoContent)
                }

                path == "/api/v1/sessions/s-2/secret" && request.method == HttpMethod.Patch -> {
                    json(
                        """{"id":"s-2","user_id":"emma","title":"Saturday","is_secret":true,"unlock_token":"chat-unlock"}""",
                    )
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
        const val ASKED = "What does the weekend of the 14th look like?"
        const val PRIVATE_WORDS = "Don't tell Liam but I want to take him to the cabin"
        const val SAFE = "Your secret's safe with me."

        private val ME =
            """{"id":"emma","full_name":"Emma Larsson","avatar_color":"#3C6E4E","is_admin":true,
            "is_active":true,"created_at":"2026-09-16T00:00:00Z"}"""

        private val ORDINARY_CHAT =
            """{"id":"s-2","user_id":"emma","title":"Saturday","is_secret":false,
            "messages":[{"id":"m-1","session_id":"s-2","role":"user","content":"$ASKED"},
            {"id":"m-2","session_id":"s-2","role":"assistant","content":"The whole weekend is open."}]}"""

        private val PRIVATE_TURN =
            """
            data: {"type": "accepted"}

            data: {"type": "delta", "content": "$SAFE"}

            data: {"type": "done", "message_id": "m-4", "assistant_content": "$SAFE", "suggest_secret_mode": true, "is_turn_secret": true}

            data: [DONE]

            """.trimIndent()
    }
}
