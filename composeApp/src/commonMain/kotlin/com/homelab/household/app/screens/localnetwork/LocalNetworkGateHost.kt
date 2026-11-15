package com.homelab.household.app.screens.localnetwork

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.homelab.household.app.components.HearthSnackbarHost
import com.homelab.household.app.components.HearthSnackbarVisuals
import com.homelab.household.app.components.SnackbarTone
import com.homelab.household.app.icons.HearthIcon
import com.homelab.household.app.platform.LocalLocalNetworkSettings
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.local_network_back_on_wifi
import com.homelab.household.app.resources.local_network_turn_on_later
import com.homelab.household.domain.localnetwork.LocalNetworkEvent
import com.homelab.household.domain.localnetwork.LocalNetworkGate
import com.homelab.household.domain.localnetwork.LocalNetworkScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject

/**
 * The app's root around local network access (#5), and its one snackbar host.
 *
 * - The explanation **replaces** [content], so nothing reaches the hub before it: on iOS the first
 *   local connection is what brings up iOS's own question.
 * - The "access is off" cover draws **over** [content], which stays composed where it was. While it
 *   covers, the screen underneath is hidden from TalkBack and VoiceOver. Back means Not now.
 * - Coming back to the app re-reads access while the cover is up. Only the return after the cover's
 *   Open settings shows Checking, then Still off; any other return hides the cover only if access is on.
 */
@Composable
fun LocalNetworkGateHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val gate = koinInject<LocalNetworkGate>()
    val settings = LocalLocalNetworkSettings.current
    val screen by gate.screen.collectAsStateWithLifecycle()
    val snackbars = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(gate) { gate.onStart() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { gate.onResumed() }
    LaunchedEffect(gate) {
        gate.events.collect { event ->
            val visuals =
                when (event) {
                    LocalNetworkEvent.BackOnWifi -> {
                        HearthSnackbarVisuals(
                            message = getString(Res.string.local_network_back_on_wifi),
                            icon = HearthIcon.Synced,
                            tone = SnackbarTone.Success,
                        )
                    }

                    LocalNetworkEvent.StoppedAsking -> {
                        HearthSnackbarVisuals(
                            message = getString(Res.string.local_network_turn_on_later),
                            icon = HearthIcon.Settings,
                            tone = SnackbarTone.Attention,
                        )
                    }
                }
            launch { snackbars.showSnackbar(visuals) }
        }
    }

    HearthSnackbarHost(hostState = snackbars, modifier = modifier) {
        when (screen) {
            LocalNetworkScreen.Starting -> {
                // Deciding at start; the decision is local and immediate. Nothing reaches the hub.
                Box(Modifier.fillMaxSize())
            }

            LocalNetworkScreen.FirstTime -> {
                LocalNetworkContent(
                    cover = LocalNetworkCover(LocalNetworkCover.Step.FirstTime, settings.platform),
                    onContinue = {
                        // The phone's own question shows over the explanation; on iOS it comes later,
                        // by itself, at the first local connection.
                        scope.launch {
                            val granted = settings.ask()
                            gate.onContinue()
                            gate.onSystemPromptAnswered(granted)
                        }
                    },
                    onOpenSettings = {},
                    onTurnOn = {},
                    onNotNow = {},
                    onDontAskAgain = {},
                )
            }

            LocalNetworkScreen.Hidden,
            LocalNetworkScreen.Off,
            LocalNetworkScreen.Checking,
            LocalNetworkScreen.StillOff,
            -> {
                val step = stepOf(screen)
                Box(Modifier.fillMaxSize()) {
                    Box(if (step != null) Modifier.clearAndSetSemantics {} else Modifier) { content() }
                    if (step != null) {
                        NavigationBackHandler(
                            state = rememberNavigationEventState(NavigationEventInfo.None),
                            onBackCompleted = gate::onNotNow,
                        )
                        // Read when the cover comes up: Android can still ask after a single "Don't allow".
                        val canAskAgain = remember(step) { settings.canAskAgain() }
                        LocalNetworkContent(
                            cover = LocalNetworkCover(step, settings.platform, canAskAgain),
                            onContinue = {},
                            onOpenSettings = {
                                // Only the return from here shows Checking, then Still off.
                                gate.onOpenSettings()
                                settings.openSettings()
                            },
                            onTurnOn = { scope.launch { gate.onSystemPromptAnswered(settings.ask()) } },
                            onNotNow = gate::onNotNow,
                            onDontAskAgain = gate::onDontAskAgain,
                        )
                    }
                }
            }
        }
    }
}

private fun stepOf(screen: LocalNetworkScreen): LocalNetworkCover.Step? =
    when (screen) {
        LocalNetworkScreen.Off -> LocalNetworkCover.Step.Off
        LocalNetworkScreen.Checking -> LocalNetworkCover.Step.Checking
        LocalNetworkScreen.StillOff -> LocalNetworkCover.Step.StillOff
        LocalNetworkScreen.Starting, LocalNetworkScreen.Hidden, LocalNetworkScreen.FirstTime -> null
    }
