package com.homelab.household.domain.repository

import com.homelab.household.domain.model.ChatMessage
import com.homelab.household.domain.model.ChatStreamEvent
import com.homelab.household.domain.model.ConversationSession
import com.homelab.household.domain.model.ProposalDetails
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun listSessions(): List<ConversationSession>

    suspend fun getSession(sessionId: String): Pair<ConversationSession, List<ChatMessage>>

    suspend fun createSession(
        agentId: String,
        title: String = "New Conversation",
        isSecret: Boolean = false,
    ): ConversationSession

    suspend fun archiveSession(sessionId: String)

    suspend fun toggleSecretMode(
        sessionId: String,
        isSecret: Boolean,
    ): ConversationSession

    suspend fun deleteSession(sessionId: String)

    /**
     * Sends a turn and follows it to an end, whatever kind of end it turns out to be.
     *
     * [afterAssistantMessageId] is the newest assistant message the caller already has, and it is
     * what makes recovery possible. When a stream dies part-way the hub keeps generating, so the
     * answer has to be fetched afterwards — but the transcript already holds the *previous*
     * answer, and "the last assistant message" matches that one immediately. Polling for an id
     * that is not this one is the difference between waiting for the real answer and handing back
     * something the person read ten minutes ago. Null means the conversation has no answers yet,
     * so any assistant message is new.
     */
    fun streamChatTurn(
        sessionId: String,
        content: String,
        afterAssistantMessageId: String? = null,
    ): Flow<ChatStreamEvent>

    /**
     * Asks for a fresh answer to the question already in the conversation, without asking it again.
     */
    fun regenerateTurn(
        sessionId: String,
        afterAssistantMessageId: String? = null,
    ): Flow<ChatStreamEvent>

    /**
     * Picks up a turn this phone stopped hearing — the screen locked, the signal went — from the
     * last word it received, or reads the saved answer when the hub has let the turn go.
     */
    fun resumeTurn(
        sessionId: String,
        afterAssistantMessageId: String? = null,
    ): Flow<ChatStreamEvent>

    /**
     * Answers one approval card and follows the rest of the turn, recovering a dropped stream the
     * way [streamChatTurn] does. The paused answer carries on as the same message.
     */
    fun decideToolProposal(
        sessionId: String,
        toolCallId: String,
        approved: Boolean,
        edited: ProposalDetails? = null,
    ): Flow<ChatStreamEvent>

    fun observeMessages(sessionId: String): Flow<List<ChatMessage>>

    suspend fun retryMessage(messageId: String): Flow<ChatStreamEvent>
}
