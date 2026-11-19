package com.homelab.household.domain.usecase

import com.homelab.household.domain.model.HeldFactAnswer

/** Save or Don't save on a fact not saved yet. It can be undone with [UndoHeldFactAnswerUseCase]. */
fun interface AnswerHeldFactUseCase {
    suspend operator fun invoke(
        heldId: String,
        answer: HeldFactAnswer,
    )
}
