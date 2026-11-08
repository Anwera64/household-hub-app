package com.homelab.household.domain.secret

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

/**
 * Whether the app hides from the app switcher: Android's recent apps, iOS's app switcher. Both keep a
 * picture of the screen, and while secret chats are unlocked that picture could show one.
 *
 * It hides the moment secret chats unlock. Leaving the app locks them (on stop), but the picture
 * outlives that, so once hidden the app shows again only when secret chats are locked **and** the
 * member is back in the app. Locking in the foreground (idle, Lock now) shows it again at once.
 *
 * [unlocked] is `SecretUnlockRepository.holding`. The platform apps report the foreground themselves:
 * resumed is [onForeground], paused is [onBackground].
 */
class AppSwitcherPrivacy(
    private val unlocked: StateFlow<Boolean>,
    scope: CoroutineScope,
) {
    private val _hides = MutableStateFlow(false)
    val hides: StateFlow<Boolean> = _hides.asStateFlow()

    // Set on the main thread, read where the unlock changes.
    @Volatile private var inForeground = true

    init {
        scope.launch { unlocked.collect { update() } }
    }

    /** The app is on screen and the member can use it. */
    fun onForeground() {
        inForeground = true
        update()
    }

    /** The app is leaving the screen: the moment the switcher's picture may be taken. */
    fun onBackground() {
        inForeground = false
    }

    private fun update() {
        _hides.update { hidden -> unlocked.value || (hidden && !inForeground) }
    }
}
