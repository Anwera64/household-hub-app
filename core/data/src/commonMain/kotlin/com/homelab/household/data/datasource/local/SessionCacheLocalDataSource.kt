package com.homelab.household.data.datasource.local

import com.homelab.household.data.dto.ChatMessageReadDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * What this phone remembers about conversations while the app is running: the messages each one has
 * shown.
 *
 * It used to be a plain `mutableMapOf` inside the session repository, written from whichever
 * coroutine happened to be streaming. It is a state flow here for two reasons. Its writes are
 * atomic, so concurrent updates cannot drop each other. And [observeMessages] can be read without
 * suspending, which the domain's `observeMessages` requires — one flow over all sessions gives
 * that, where a map of per-session flows needed a lock to create an entry safely.
 */
class SessionCacheLocalDataSource {
    private val messagesBySession = MutableStateFlow<Map<String, List<ChatMessageReadDto>>>(emptyMap())

    // ---- messages ----------------------------------------------------------

    fun observeMessages(sessionId: String): Flow<List<ChatMessageReadDto>> =
        messagesBySession.map { it[sessionId].orEmpty() }.distinctUntilChanged()

    fun cacheMessages(
        sessionId: String,
        messages: List<ChatMessageReadDto>,
    ) {
        messagesBySession.update { it + (sessionId to messages) }
    }

    /** The conversation a remembered message belongs to, for retrying it without asking the hub. */
    fun sessionHolding(messageId: String): Pair<String, ChatMessageReadDto>? =
        messagesBySession.value.firstNotNullOfOrNull { (sessionId, messages) ->
            messages.firstOrNull { it.id == messageId }?.let { sessionId to it }
        }
}
