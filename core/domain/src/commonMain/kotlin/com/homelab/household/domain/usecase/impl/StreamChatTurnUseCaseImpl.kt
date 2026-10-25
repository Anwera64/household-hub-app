package com.homelab.household.domain.usecase.impl

import com.homelab.household.domain.exception.ValidationException
import com.homelab.household.domain.model.ChatStreamEvent
import com.homelab.household.domain.repository.SessionRepository
import com.homelab.household.domain.usecase.StreamChatTurnUseCase
import kotlinx.coroutines.flow.Flow

class StreamChatTurnUseCaseImpl(
    private val sessionRepository: SessionRepository,
) : StreamChatTurnUseCase {
    override operator fun invoke(
        sessionId: String,
        content: String,
        afterAssistantMessageId: String?,
    ): Flow<ChatStreamEvent> {
        if (sessionId.isBlank()) throw ValidationException("Session ID cannot be blank")
        if (content.isBlank()) throw ValidationException("Message content cannot be blank")
        return sessionRepository.streamChatTurn(
            sessionId = sessionId,
            content = content.trim(),
            afterAssistantMessageId = afterAssistantMessageId,
        )
    }
}
