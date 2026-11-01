package com.homelab.household.data.datasource.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * The unlocks this phone holds, in memory and nowhere else: a restart starts locked.
 *
 * Two kinds. The PIN's opens every secret chat; a chat's own comes back when that chat is made
 * secret, or started secret, and opens only it (locking spec §4.1.1). Both age, and both renew to
 * their own kind. Read by the header plugin for every chats call, so it can't sit behind the
 * repository that calls the hub through that plugin.
 */
class SecretUnlockLocalDataSource(
    private val clock: TimeSource = TimeSource.Monotonic,
) {
    private data class Held(
        val token: String,
        val issued: TimeMark,
    )

    private val allChats = MutableStateFlow<Held?>(null)
    private val chats = MutableStateFlow<Map<String, Held>>(emptyMap())
    private val held = MutableStateFlow(false)
    private val forgotten = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Whether any unlock is held: the PIN's, or a chat's own. */
    val holding: StateFlow<Boolean> = held.asStateFlow()

    /** Each time every unlock is forgotten. */
    val locks: SharedFlow<Unit> = forgotten.asSharedFlow()

    fun hold(token: String) {
        allChats.value = Held(token, clock.markNow())
        update()
    }

    fun holdChat(
        sessionId: String,
        token: String,
    ) {
        chats.update { it + (sessionId to Held(token, clock.markNow())) }
        update()
    }

    /** The unlock to send for [sessionId]: the PIN's when held, otherwise that chat's own. */
    fun tokenFor(sessionId: String?): String? = allChats.value?.token ?: sessionId?.let { chats.value[it]?.token }

    /** Every unlock held, with the chat it opens; null opens every secret chat. */
    fun heldTokens(): List<Pair<String?, String>> =
        listOfNotNull(allChats.value?.let { null to it.token }) + chats.value.map { (chat, held) -> chat to held.token }

    /** How long ago the oldest unlock held was issued or renewed; null when none is held. */
    fun age(): Duration? = (listOfNotNull(allChats.value) + chats.value.values).maxOfOrNull { it.issued.elapsedNow() }

    /** The hub refused [token]: forget it, wherever it was held. */
    fun forget(token: String) {
        allChats.update { held -> held?.takeUnless { it.token == token } }
        chats.update { held -> held.filterValues { it.token != token } }
        update()
    }

    fun forgetAll() {
        allChats.value = null
        chats.value = emptyMap()
        update()
        forgotten.tryEmit(Unit)
    }

    private fun update() {
        held.value = allChats.value != null || chats.value.isNotEmpty()
    }
}
