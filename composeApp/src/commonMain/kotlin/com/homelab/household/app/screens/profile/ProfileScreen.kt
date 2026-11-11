package com.homelab.household.app.screens.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homelab.household.app.platform.LocalLocalNetworkSettings
import com.homelab.household.app.util.ObserveEvents
import com.homelab.household.domain.localnetwork.LocalNetworkGate
import com.homelab.household.presentation.profile.ProfileEvent
import com.homelab.household.presentation.profile.ProfileViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Your own account: your calendar, what agents may do, local network access while it is off, the
 * members list, your PIN, and the two ways out.
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onMembers: () -> Unit,
    onChangePin: () -> Unit,
    onLeave: () -> Unit,
    onSignedOut: () -> Unit,
    onCalendar: () -> Unit,
    onChooseCalendars: () -> Unit,
    onCalendarSignInAgain: () -> Unit,
    onWhenAgentsAct: () -> Unit,
    onMemories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ProfileViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val localNetwork = LocalLocalNetworkSettings.current
    val gate = koinInject<LocalNetworkGate>()
    val scope = rememberCoroutineScope()

    // On every return too, so coming back from Settings with access on drops the row.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.loadLocalNetwork() }

    // Every time the profile is shown, not once: coming back from connecting a calendar shows it,
    // coming back from "When agents act" counts what was just switched, and coming back from the
    // memory page counts what was just removed.
    LaunchedEffect(viewModel) {
        viewModel.loadCalendar()
        viewModel.loadAutomaticWrites()
        viewModel.loadMemoryCounts()
    }

    ObserveEvents(viewModel.events) { event ->
        when (event) {
            ProfileEvent.SignedOut -> onSignedOut()
        }
    }

    ProfileContent(
        state = state,
        onBack = onBack,
        onMembers = onMembers,
        onChangePin = onChangePin,
        onLeave = onLeave,
        onSignOut = viewModel::onSignOut,
        onCalendar = onCalendar,
        onChooseCalendars = onChooseCalendars,
        onCalendarSignInAgain = onCalendarSignInAgain,
        onWhenAgentsAct = onWhenAgentsAct,
        onMemories = onMemories,
        onLocalNetwork = {
            if (localNetwork.canAskAgain()) {
                scope.launch {
                    gate.onSystemPromptAnswered(localNetwork.ask())
                    viewModel.loadLocalNetwork()
                }
            } else {
                localNetwork.openSettings()
            }
        },
        modifier = modifier,
        onDisconnectCalendar = viewModel::onDisconnectCalendar,
    )
}
