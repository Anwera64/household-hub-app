package com.homelab.household.data.network

import com.homelab.household.data.datasource.local.SecretUnlockLocalDataSource
import com.homelab.household.domain.exception.SecretLockedException
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpStatusCode

/** The header the hub reads an unlock from (locking spec §4.2). */
const val SECRET_UNLOCK_HEADER = "X-Secret-Unlock"

private const val CHATS_PATH = "/api/v1/sessions"
private const val RENEW_PATH = "/api/v1/auth/unlock-secret/renew"

/**
 * Sends the unlock [unlocks] holds with the calls that read or extend a chat, and with its renewal.
 * No other call carries it, so an unlock never travels further than it has to.
 *
 * A 423 is the hub saying the chat is locked: the unlock that went with the call is forgotten and
 * the call throws [SecretLockedException]. The member stays signed in; a lock is never a 401.
 */
fun HttpClientConfig<*>.sendSecretUnlock(unlocks: SecretUnlockLocalDataSource) {
    val plugin =
        createClientPlugin("SecretUnlock") {
            onRequest { request, _ ->
                val path = request.url.build().encodedPath
                val token =
                    when {
                        path == RENEW_PATH -> unlocks.tokenFor(null)
                        path == CHATS_PATH || path.startsWith("$CHATS_PATH/") -> unlocks.tokenFor(chatIn(path))
                        else -> null
                    }
                token?.let { request.headers[SECRET_UNLOCK_HEADER] = it }
            }
        }
    install(plugin)
    HttpResponseValidator {
        validateResponse { response ->
            if (response.status == HttpStatusCode.Locked) {
                response.call.request.headers[SECRET_UNLOCK_HEADER]?.let(unlocks::forget)
                throw SecretLockedException()
            }
        }
    }
}

/** The chat a `/api/v1/sessions/{id}/…` path is about; null for the list itself. */
private fun chatIn(path: String): String? =
    path
        .removePrefix("$CHATS_PATH/")
        .takeIf { it != path }
        ?.substringBefore('/')
        ?.takeIf { it.isNotEmpty() }
