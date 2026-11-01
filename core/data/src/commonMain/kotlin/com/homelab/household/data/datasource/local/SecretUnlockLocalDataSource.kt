package com.homelab.household.data.datasource.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * The unlocks this phone holds, in memory and nowhere else: a restart starts locked.
 *
 * Two kinds. The PIN's opens every secret chat; a chat's own comes back when that chat is made
 * secret, or started secret, and opens only it (locking spec §4.1.1). Read by the header plugin for
 * every chats call, so it can't sit behind the repository that calls the hub through that plugin.
 */
class SecretUnlockLocalDataSource(
    private val clock: TimeSource = TimeSource.Monotonic,
) {
    private data class Held(
        val token: String,
        val issued: TimeMark,
    )

    private val allChats = MutableStateFlow<Held?>(null)
    private val chats = MutableStateFlow<Map<String, String>>(emptyMap())
    private val open = MutableStateFlow(false)

    /** Whether the PIN's unlock is held. */
    val unlocked: StateFlow<Boolean> = open.asStateFlow()

    fun hold(token: String) {
        allChats.value = Held(token, clock.markNow())
        open.value = true
    }

    fun holdChat(
        sessionId: String,
        token: String,
    ) {
        chats.update { it + (sessionId to token) }
    }

    /** The unlock to send for [sessionId]: the PIN's when held, otherwise that chat's own. */
    fun tokenFor(sessionId: String?): String? = allChats.value?.token ?: sessionId?.let { chats.value[it] }

    /** How long ago the PIN's unlock was issued; null when none is held. */
    fun age(): Duration? = allChats.value?.issued?.elapsedNow()

    /** The hub refused [token]: forget it, wherever it was held. */
    fun forget(token: String) {
        allChats.update { held -> held?.takeUnless { it.token == token } }
        chats.update { held -> held.filterValues { it != token } }
        open.value = allChats.value != null
    }

    fun forgetAll() {
        allChats.value = null
        chats.value = emptyMap()
        open.value = false
    }
}
