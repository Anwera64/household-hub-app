package com.homelab.household.app.screens.launch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homelab.household.app.platform.LocalExternalApps
import com.homelab.household.app.platform.LocalLocalNetworkSettings
import com.homelab.household.app.util.ObserveEvents
import com.homelab.household.domain.localnetwork.LocalNetworkGate
import com.homelab.household.presentation.launch.HubFailure
import com.homelab.household.presentation.launch.HubStatus
import com.homelab.household.presentation.launch.LaunchEvent
import com.homelab.household.presentation.launch.LaunchViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Launch asks the hub where it stands the moment it appears, and moves on once it knows. A hub
 * that can't be read keeps the user here, counting down to asking again, with Tailscale one tap
 * away. Only a phone with nobody signed in opens here.
 *
 * Its offline screen carries its own reason and fix, local network access among them (#5), so the
 * "access is off" cover never draws over launch.
 */
@Composable
fun LaunchScreen(
    onSignIn: () -> Unit,
    onFirstRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LaunchViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val externalApps = LocalExternalApps.current
    val localNetwork = LocalLocalNetworkSettings.current
    val gate = koinInject<LocalNetworkGate>()
    val scope = rememberCoroutineScope()

    DisposableEffect(gate) {
        gate.coversAllowed(false)
        onDispose { gate.coversAllowed(true) }
    }

    val localNetworkOff = (state.status as? HubStatus.Unavailable)?.reason == HubFailure.LocalNetworkOff
    // Back from Settings: ask again now rather than at the end of the countdown.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (localNetworkOff) viewModel.checkHub() }

    ObserveEvents(viewModel.events) { event ->
        when (event) {
            LaunchEvent.GoToSignIn -> onSignIn()
            LaunchEvent.GoToFirstRun -> onFirstRun()
        }
    }

    val canAskAgain = remember(localNetworkOff) { localNetworkOff && localNetwork.canAskAgain() }
    LaunchContent(
        state = state,
        onRetry = viewModel::checkHub,
        onOpenTailscale = externalApps::openTailscale,
        localNetworkFix =
            LocalNetworkFix(
                platform = localNetwork.platform,
                canAskAgain = canAskAgain,
                onOpenSettings = localNetwork::openSettings,
                onTurnOn = {
                    scope.launch {
                        val granted = localNetwork.ask()
                        gate.onSystemPromptAnswered(granted)
                        if (granted) viewModel.checkHub()
                    }
                },
            ),
        modifier = modifier,
    )
}
