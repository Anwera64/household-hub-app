package com.homelab.household.domain.secret

import com.homelab.household.domain.exception.SecretLockedException
import com.homelab.household.domain.repository.SecretUnlockRepository
import com.homelab.household.domain.util.runCatchingSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

/**
 * When secret chats lock again (locking spec §4.1): ten minutes without the member's own input, the
 * app leaving the screen, or signing out.
 *
 * Only [onUserInput] keeps them open. Stream events, polls and syncs never reach this class, so an
 * answer arriving on an unattended phone can't keep a secret chat readable. Locking only forgets the
 * unlock: a stream already running finishes, and the next call that needs the unlock gets a 423.
 */
class SecretLockController(
    private val unlocks: SecretUnlockRepository,
    signedOut: Flow<Unit>,
    private val scope: CoroutineScope,
) {
    private var idle: Job? = null
    private var renewal: Job? = null

    init {
        scope.launch { signedOut.collect { lock() } }
        scope.launch {
            unlocks.unlocked.collect { open ->
                if (open) restartIdle() else idle?.cancel()
            }
        }
    }

    /** A tap, a scroll, a keystroke or a send: the member is here. */
    fun onUserInput() {
        if (!unlocks.unlocked.value) return
        restartIdle()
        val age = unlocks.tokenAge() ?: return
        if (age > RENEW_AFTER && renewal?.isActive != true) {
            renewal =
                scope.launch {
                    runCatchingSafe { unlocks.renew() }
                        .onFailure { if (it is SecretLockedException) lock() }
                }
        }
    }

    /** The app left the screen: backgrounded, or the device locked. */
    fun onAppStopped() = lock()

    private fun lock() {
        idle?.cancel()
        unlocks.lock()
    }

    private fun restartIdle() {
        idle?.cancel()
        idle =
            scope.launch {
                delay(IDLE_LIMIT)
                unlocks.lock()
            }
    }

    private companion object {
        val IDLE_LIMIT = 10.minutes

        /** Renewing on every tap would be a call per tap; the hub's ten minutes leave room for this. */
        val RENEW_AFTER = 1.minutes
    }
}
