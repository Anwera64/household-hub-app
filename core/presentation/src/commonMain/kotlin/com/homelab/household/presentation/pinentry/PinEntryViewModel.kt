package com.homelab.household.presentation.pinentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homelab.household.domain.model.Member
import com.homelab.household.domain.usecase.LoginUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class PinEntryViewModel(
    member: Member,
    private val loginUseCase: LoginUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PinEntryUiState(member = member))
    val uiState: StateFlow<PinEntryUiState> = _uiState.asStateFlow()

    private val _events = Channel<PinEntryEvent>(Channel.BUFFERED)
    val events: Flow<PinEntryEvent> = _events.receiveAsFlow()

    private val pad =
        PinPadInput(
            scope = viewModelScope,
            check = { pin -> loginUseCase(_uiState.value.member.id, pin) },
            onAccepted = { _events.send(PinEntryEvent.SignedIn) },
            show = { entered, status -> _uiState.update { it.copy(entered = entered, status = status) } },
        )

    /** The sixth digit sends the PIN; there is no submit button. */
    fun onDigit(digit: Char) = pad.onDigit(digit)

    fun onDelete() = pad.onDelete()
}
