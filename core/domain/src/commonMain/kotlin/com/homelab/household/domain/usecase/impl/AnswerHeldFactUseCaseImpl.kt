package com.homelab.household.domain.usecase.impl

import com.homelab.household.domain.model.HeldFactAnswer
import com.homelab.household.domain.repository.MemoryRepository
import com.homelab.household.domain.usecase.AnswerHeldFactUseCase

class AnswerHeldFactUseCaseImpl(
    private val memoryRepository: MemoryRepository,
) : AnswerHeldFactUseCase {
    override suspend operator fun invoke(
        heldId: String,
        answer: HeldFactAnswer,
    ) = memoryRepository.answerHeldFact(heldId, answer)
}
