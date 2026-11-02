package com.homelab.household.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

/**
 * The unlock that opens secret chats on this phone (locking spec §4.1).
 *
 * Held in memory only: a phone that restarts starts locked, which is the posture the spec asks for.
 * With nothing held, every secret chat is locked; the hub answers 423 and the call throws
 * `SecretLockedException`.
 */
interface SecretUnlockRepository {
    /** Whether any unlock is held: the PIN's, or a chat's own. While one is, the idle timer runs. */
    val holding: StateFlow<Boolean>

    /** Each time every unlock is forgotten, so a secret chat on screen can lock in place. */
    val locks: Flow<Unit>

    /** Trades the member's PIN for an unlock. Wrong PINs throw as signing in does. */
    suspend fun unlock(pin: String)

    /** A fresh ten minutes for every unlock held. Throws `SecretLockedException` when the hub has let one go. */
    suspend fun renew()

    /** Forgets every unlock on this phone. The hub's copy runs out on its own within ten minutes. */
    fun lock()

    /**
     * The unlock the hub hands back when a chat is made secret, or started secret: it opens that
     * chat alone, so the member is not sent to the PIN mid-conversation (locking spec §4.1.1).
     */
    fun holdChatUnlock(
        sessionId: String,
        token: String,
    )

    /** How long ago the oldest unlock held was issued or renewed; null when none is held. */
    fun tokenAge(): Duration?
}
