package com.homelab.household.presentation.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homelab.household.domain.model.ConversationSession
import com.homelab.household.domain.usecase.ListSessionsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The Chats tab: the conversations, and the search that runs over them.
 *
 * Search is on-device for the MVP. The list the hub returns is already the whole list, so
 * filtering it here costs nothing, works offline over whatever is loaded, and needed no backend
 * work at all — which is why the screen's footnote says plainly what it cannot do, so a miss is
 * not mistaken for a lost chat. Full-text search comes after the MVP.
 */
class ChatsViewModel(
    private val listSessionsUseCase: ListSessionsUseCase,
    /** Throws away every unlock on this phone: the bar's Lock now. */
    private val lockSecretChats: () -> Unit = {},
    /** Each time secret chats lock again: Lock now, idle, the app leaving the screen, signing out. */
    secretLocks: Flow<Unit> = emptyFlow(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatsUiState())
    val uiState: StateFlow<ChatsUiState> = _uiState.asStateFlow()

    private var all: List<ConversationSession> = emptyList()

    init {
        viewModelScope.launch {
            secretLocks.collect {
                holdBackSecretTitles()
                load()
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                all = listSessionsUseCase()
                _uiState.update {
                    it.copy(isLoading = false, loaded = true, total = all.size, visible = matching(it.query))
                }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load chats")
                }
            }
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(query = query, visible = matching(query)) }
    }

    fun cancelSearch() = search("")

    fun lockNow() = lockSecretChats()

    /**
     * The titles on screen came with an unlock that is gone. They go at once, before the hub is
     * asked again, so a hub that cannot be reached does not leave them showing.
     */
    private fun holdBackSecretTitles() {
        all =
            all.map { session ->
                if (session.isSecret || session.hasSecretPart) {
                    session.copy(title = "", lastMessagePreview = null, isLocked = true)
                } else {
                    session
                }
            }
        _uiState.update { it.copy(visible = matching(it.query)) }
    }

    /**
     * Secret chats are never a search result, locked or not, and nor is a chat that holds a secret
     * part — a title you can find is a title you have read. Off a blank query the list is whole,
     * secret rows included, because that is the list itself rather than a search of it.
     */
    private fun matching(query: String): List<ConversationSession> {
        if (query.isBlank()) return all
        return all.filter { session ->
            !session.isSecret && !session.hasSecretPart &&
                (
                    session.title.contains(query, ignoreCase = true) ||
                        session.agentName?.contains(query, ignoreCase = true) == true
                )
        }
    }
}
