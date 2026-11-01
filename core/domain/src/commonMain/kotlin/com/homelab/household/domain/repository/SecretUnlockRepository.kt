package com.homelab.household.domain.repository

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
    /** Whether the PIN has opened every secret chat. A chat's own unlock doesn't count here. */
    val unlocked: StateFlow<Boolean>

    /** Trades the member's PIN for an unlock. Wrong PINs throw as signing in does. */
    suspend fun unlock(pin: String)

    /** A fresh ten minutes for the unlock held. Throws `SecretLockedException` when the hub has let it go. */
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

    /** How long ago the PIN's unlock was issued or renewed; null when none is held. */
    fun tokenAge(): Duration?
}
