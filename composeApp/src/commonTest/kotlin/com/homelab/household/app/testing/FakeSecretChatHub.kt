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
 * `s-1` is read whole, as a hub before section reads did; `s-3` is the same chat on a hub that reads
 * by section: it answers 200 without the unlock, every message withheld.
 *
 * `s-2` was secret and turned off again: it opens without the unlock, its secret part withheld.
 * Turning `s-1` off makes it the same kind of chat. `s-4` had ordinary messages and then Secret Mode
 * on, still on: it too opens without the unlock, its secret part withheld.
 *
 * The chats list holds `s-1` back the same way: locked, with an empty title, until the unlock comes
 * with it. Its last message is ordinary, so its preview is sent either way.
 */
class FakeSecretChatHub {
    var unlockAttempts = 0
        private set

    /** What each `PATCH …/secret` asked for, in order. */
    val secretToggles = mutableListOf<String>()

    /** How many sends reached `s-4`'s chat stream. */
    var secretSends = 0
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

                path == "/api/v1/sessions/s-1/secret" && request.method == HttpMethod.Patch -> {
                    secretToggles += (request.body as? TextContent)?.text.orEmpty()
                    json(SECRET_CHAT_TURNED_OFF)
                }

                path == "/api/v1/sessions/s-2" && request.method == HttpMethod.Get -> {
                    json(if (request.headers["X-Secret-Unlock"] == UNLOCK) TURNED_OFF_CHAT else TURNED_OFF_CHAT_LOCKED)
                }

                path == "/api/v1/sessions/s-3" && request.method == HttpMethod.Get -> {
                    json(startedSecretChat(withheld = request.headers["X-Secret-Unlock"] != UNLOCK))
                }

                path == "/api/v1/sessions/s-4/chat/stream" && request.method == HttpMethod.Post -> {
                    // The fallback for an unlock the hub let go: a phone holding none asks for
                    // the PIN first, so a send that gets here is refused as locked.
                    secretSends++
                    json("""{"detail":"locked","code":"secret_locked"}""", HttpStatusCode.Locked)
                }

                path == "/api/v1/sessions/s-4" && request.method == HttpMethod.Get -> {
                    json(turnedOnChat(withheld = request.headers["X-Secret-Unlock"] != UNLOCK))
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
        const val SECRET_ANSWER = "Can Pere does a set menu on Saturdays"
        const val ASKED_BEFORE = "What's on this weekend?"
        const val ASKED_AFTER = "Add the plumber on Monday"
        const val ANSWERED_BEFORE = "Two markets and a concert"
        const val ORDINARY_TITLE = "Groceries for the week"
        const val SECRET_TITLE = "Anniversary surprise"
        const val SECRET_PREVIEW = "Booked the table for eight"

        private val ME =
            """{"id":"emma","full_name":"Emma Larsson","avatar_color":"#3C6E4E","is_admin":true,
            "is_active":true,"created_at":"2026-09-16T00:00:00Z"}"""

        private val SECRET_CHAT_TURNED_OFF =
            """{"id":"s-1","user_id":"emma","title":"Saturday","is_secret":false,"has_secret_part":true}"""

        private fun turnedOffChat(withheld: Boolean): String {
            fun secret(
                id: String,
                role: String,
                content: String,
            ) = if (withheld) {
                """{"id":"$id","session_id":"s-2","role":"$role","content":"","is_secret":true,"withheld":true}"""
            } else {
                """{"id":"$id","session_id":"s-2","role":"$role","content":"$content","is_secret":true}"""
            }
            return """{"id":"s-2","user_id":"emma","title":"Weekend","is_secret":false,"has_secret_part":true,
            "messages":[{"id":"m-1","session_id":"s-2","role":"user","content":"$ASKED_BEFORE"},
            ${secret("m-2", "user", SECRET_WORDS)},${secret("m-3", "assistant", SECRET_ANSWER)},
            {"id":"m-4","session_id":"s-2","role":"user","content":"$ASKED_AFTER"}]}"""
        }

        /** A message of [sessionId] said in secret: its words, or held back without the unlock. */
        private fun secretMessage(
            sessionId: String,
            id: String,
            role: String,
            content: String,
            withheld: Boolean,
        ) = if (withheld) {
            """{"id":"$id","session_id":"$sessionId","role":"$role","content":"","is_secret":true,"withheld":true}"""
        } else {
            """{"id":"$id","session_id":"$sessionId","role":"$role","content":"$content","is_secret":true}"""
        }

        private fun startedSecretChat(withheld: Boolean) =
            """{"id":"s-3","user_id":"emma","title":"Saturday","is_secret":true,"has_secret_part":true,
            "messages":[${secretMessage("s-3", "m-1", "user", SECRET_WORDS, withheld)},
            ${secretMessage("s-3", "m-2", "assistant", SECRET_ANSWER, withheld)}]}"""

        private fun turnedOnChat(withheld: Boolean) =
            """{"id":"s-4","user_id":"emma","title":"Weekend","is_secret":true,"has_secret_part":true,
            "messages":[{"id":"m-1","session_id":"s-4","role":"user","content":"$ASKED_BEFORE"},
            {"id":"m-2","session_id":"s-4","role":"assistant","content":"$ANSWERED_BEFORE"},
            ${secretMessage("s-4", "m-3", "user", SECRET_WORDS, withheld)},
            ${secretMessage("s-4", "m-4", "assistant", SECRET_ANSWER, withheld)}]}"""

        private val TURNED_OFF_CHAT_LOCKED = turnedOffChat(withheld = true)
        private val TURNED_OFF_CHAT = turnedOffChat(withheld = false)

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
