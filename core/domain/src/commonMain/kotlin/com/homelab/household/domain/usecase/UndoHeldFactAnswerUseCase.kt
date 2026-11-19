package com.homelab.household.domain.usecase

/** Undo after answering a fact not saved yet: it waits for an answer again. */
fun interface UndoHeldFactAnswerUseCase {
    suspend operator fun invoke(heldId: String)
}
