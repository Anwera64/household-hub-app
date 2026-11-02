package com.homelab.household.presentation.secretunlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homelab.household.domain.usecase.UnlockSecretChatsUseCase
import com.homelab.household.presentation.pinentry.PinPadInput
import com.homelab.household.presentation.pinentry.PinStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/** What the locked chat's PIN pad draws: how many digits are in, never which, and where it stands. */
data class SecretUnlockUiState(
    val entered: Int = 0,
    val status: PinStatus = PinStatus.Idle,
)

sealed interface SecretUnlockEvent {
    /** The PIN opened every secret chat; the screen shows the chat it was locked on. */
    data object Unlocked : SecretUnlockEvent
}

/** The locked chat's PIN pad: sign-in's pad, sending the PIN to unlock secret chats. */
class SecretUnlockViewModel(
    unlock: UnlockSecretChatsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SecretUnlockUiState())
    val uiState: StateFlow<SecretUnlockUiState> = _uiState.asStateFlow()

    private val _events = Channel<SecretUnlockEvent>(Channel.BUFFERED)
    val events: Flow<SecretUnlockEvent> = _events.receiveAsFlow()

    private val pad =
        PinPadInput(
            scope = viewModelScope,
            check = { pin -> unlock(pin) },
            onAccepted = { _events.send(SecretUnlockEvent.Unlocked) },
            show = { entered, status -> _uiState.value = SecretUnlockUiState(entered, status) },
        )

    fun onDigit(digit: Char) = pad.onDigit(digit)

    fun onDelete() = pad.onDelete()
}
