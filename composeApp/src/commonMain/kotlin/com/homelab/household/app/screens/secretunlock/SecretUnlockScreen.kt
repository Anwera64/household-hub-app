package com.homelab.household.app.screens.secretunlock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homelab.household.app.util.ObserveEvents
import com.homelab.household.presentation.secretunlock.SecretUnlockEvent
import com.homelab.household.presentation.secretunlock.SecretUnlockViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * The secret chats PIN pad, a destination of its own over Chats or over a chat. The sixth digit
 * unlocks, and [onUnlock] goes back to where it was opened. Its ViewModel lives and dies with the
 * back stack entry, so each time it opens it starts empty.
 */
@Composable
fun SecretUnlockScreen(
    onUnlock: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SecretUnlockViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveEvents(viewModel.events) { event ->
        when (event) {
            SecretUnlockEvent.Unlocked -> onUnlock()
        }
    }

    SecretUnlockContent(
        state = state,
        onDigit = viewModel::onDigit,
        onDelete = viewModel::onDelete,
        onBack = onBack,
        modifier = modifier,
    )
}
