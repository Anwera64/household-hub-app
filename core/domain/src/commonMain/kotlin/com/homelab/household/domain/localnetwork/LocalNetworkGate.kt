package com.homelab.household.domain.localnetwork

import com.homelab.household.domain.repository.LocalNetworkRepository
import com.homelab.household.domain.usecase.HasStoredSessionUseCase
import com.homelab.household.domain.util.runCatchingSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

/**
 * When the app explains local network access, and when it says access is off (#5).
 *
 * - **First time**, only at start and before anything reaches the hub: on Android when access is
 *   off and the app never explained it; on iOS, which can't be asked, on a new install only (no
 *   explanation stored and nobody signed in). A phone already signed in has used the app, so iOS
 *   has asked it: it counts as explained from then on.
 * - **Access is off**, only once a hub call found no route while access is off. Never while the hub
 *   answers, as it does over Tailscale away from home, and never over launch's own offline screen.
 * - **Not now** (or a "no" to the phone's own question) keeps it quiet for this process. **Don't ask
 *   again** keeps it quiet on this phone.
 *
 * One for the app, like `SecretLockController`. The app reports start, resume and the member's
 * answers; hub failures come in through [hubUnreachable]. At most one read of access runs at a time.
 */
class LocalNetworkGate(
    private val access: LocalNetworkAccess,
    private val choices: LocalNetworkRepository,
    private val hasStoredSession: HasStoredSessionUseCase,
    hubUnreachable: Flow<Unit>,
    private val scope: CoroutineScope,
) {
    private val _screen = MutableStateFlow(LocalNetworkScreen.Starting)
    val screen: StateFlow<LocalNetworkScreen> = _screen.asStateFlow()

    private val _events = Channel<LocalNetworkEvent>(Channel.BUFFERED)

    /** Each event goes to one collector, once: the app root's snackbar. */
    val events: Flow<LocalNetworkEvent> = _events.receiveAsFlow()

    // Set on the main thread, read where a hub failure lands.
    @Volatile private var quietUntilRestart = false

    @Volatile private var coversAllowed = true

    private var check: Job? = null

    init {
        scope.launch { hubUnreachable.collect { onHubUnreachable() } }
    }

    /** The app is starting. Decides only whether to explain first; once per process. */
    fun onStart() {
        if (_screen.value != LocalNetworkScreen.Starting) return
        if (choices.explained() || choices.stoppedAsking()) {
            _screen.value = LocalNetworkScreen.Hidden
            return
        }
        if (!access.readsWithoutAsking) {
            if (hasStoredSession()) {
                choices.markExplained()
                _screen.value = LocalNetworkScreen.Hidden
            } else {
                _screen.value = LocalNetworkScreen.FirstTime
            }
            return
        }
        scope.launch {
            val status = runCatchingSafe { access.status() }.getOrDefault(LocalNetworkStatus.Unknown)
            _screen.value =
                if (status == LocalNetworkStatus.Off) LocalNetworkScreen.FirstTime else LocalNetworkScreen.Hidden
        }
    }

    /** Continue, on the explanation. The phone's own question comes next, from the app. */
    fun onContinue() {
        choices.markExplained()
        _screen.value = LocalNetworkScreen.Hidden
    }

    /** The member answered the phone's own question. A "no" counts as Not now. */
    fun onSystemPromptAnswered(granted: Boolean) {
        if (!granted) {
            onNotNow()
            return
        }
        val covering = _screen.value.isCover()
        _screen.value = LocalNetworkScreen.Hidden
        if (covering) _events.trySend(LocalNetworkEvent.BackOnWifi)
    }

    /** Quiet until the app really restarts: a new process, not a return from the background. */
    fun onNotNow() {
        quietUntilRestart = true
        hideCover()
    }

    fun onDontAskAgain() {
        choices.stopAsking()
        hideCover()
        _events.trySend(LocalNetworkEvent.StoppedAsking)
    }

    /**
     * Back in the app, from Settings or anywhere else. Reads access again only while a cover is up,
     * or, on Android, while the explanation is: access turned on meanwhile needs no explaining.
     */
    fun onResumed() {
        val before = _screen.value
        if (before == LocalNetworkScreen.FirstTime && access.readsWithoutAsking) {
            read { status ->
                if (status == LocalNetworkStatus.Allowed || status == LocalNetworkStatus.NotNeeded) {
                    if (_screen.value == LocalNetworkScreen.FirstTime) _screen.value = LocalNetworkScreen.Hidden
                }
            }
            return
        }
        if (!before.isCover() || check?.isActive == true) return
        _screen.value = LocalNetworkScreen.Checking
        read { status ->
            when (status) {
                LocalNetworkStatus.Allowed, LocalNetworkStatus.NotNeeded -> {
                    _screen.value = LocalNetworkScreen.Hidden
                    _events.trySend(LocalNetworkEvent.BackOnWifi)
                }

                LocalNetworkStatus.Off -> {
                    _screen.value = LocalNetworkScreen.StillOff
                }

                LocalNetworkStatus.Unknown -> {
                    _screen.value = before
                }
            }
        }
    }

    /** False while launch shows its own offline screen, which carries its own reason and fix. */
    fun coversAllowed(allowed: Boolean) {
        coversAllowed = allowed
        if (!allowed) hideCover()
    }

    private fun onHubUnreachable() {
        if (quietUntilRestart || !coversAllowed || choices.stoppedAsking()) return
        if (_screen.value != LocalNetworkScreen.Hidden) return
        read { status ->
            if (status == LocalNetworkStatus.Off && coversAllowed && !quietUntilRestart &&
                _screen.value == LocalNetworkScreen.Hidden
            ) {
                _screen.value = LocalNetworkScreen.Off
            }
        }
    }

    private fun read(then: (LocalNetworkStatus) -> Unit) {
        if (check?.isActive == true) return
        check =
            scope.launch {
                then(runCatchingSafe { access.status() }.getOrDefault(LocalNetworkStatus.Unknown))
            }
    }

    private fun hideCover() {
        check?.cancel()
        if (_screen.value.isCover()) _screen.value = LocalNetworkScreen.Hidden
    }

    private fun LocalNetworkScreen.isCover() =
        this == LocalNetworkScreen.Off || this == LocalNetworkScreen.Checking || this == LocalNetworkScreen.StillOff
}
