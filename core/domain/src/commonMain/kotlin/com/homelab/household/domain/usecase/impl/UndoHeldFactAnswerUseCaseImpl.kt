package com.homelab.household.domain.usecase.impl

import com.homelab.household.domain.repository.MemoryRepository
import com.homelab.household.domain.usecase.UndoHeldFactAnswerUseCase

class UndoHeldFactAnswerUseCaseImpl(
    private val memoryRepository: MemoryRepository,
) : UndoHeldFactAnswerUseCase {
    override suspend operator fun invoke(heldId: String) = memoryRepository.undoHeldFactAnswer(heldId)
}
