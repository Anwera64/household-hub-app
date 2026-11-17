package com.homelab.household.presentation.memories

import com.homelab.household.domain.exception.HeldFactGoneException
import com.homelab.household.domain.model.DeviceTimeZone
import com.homelab.household.domain.model.HeldFact
import com.homelab.household.domain.model.HeldFactAnswer
import com.homelab.household.domain.usecase.AnswerHeldFactUseCase
import com.homelab.household.domain.usecase.UndoHeldFactAnswerUseCase
import com.homelab.household.domain.util.runCatchingSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Where a fact not saved yet is: open to answer, saving one answer, or answered with Undo in place. */
sealed interface HeldStatus {
    /** [failed]: the last answer didn't save; "Couldn't save your choice. Nothing changed." */
    data class Open(
        val failed: Boolean = false,
    ) : HeldStatus

    data class Saving(
        val answer: HeldFactAnswer,
    ) : HeldStatus

    /** [undoFailed]: Undo didn't reach the hub, so the answer stands. */
    data class Done(
        val answer: HeldFactAnswer,
        val undoFailed: Boolean = false,
    ) : HeldStatus
}

/** One fact not saved yet as its card draws it: when it was read, and its "until" day, on the member's phone. */
data class HeldRow(
    val fact: HeldFact,
    val readAge: MemoryAge,
    val readTime: LocalTime,
    val until: LocalDay?,
    val status: HeldStatus = HeldStatus.Open(),
)

@OptIn(ExperimentalTime::class)
fun heldRowOf(
    fact: HeldFact,
    deviceTimeZone: DeviceTimeZone,
    clock: Clock,
): HeldRow {
    val now = clock.now()
    return HeldRow(
        fact = fact,
        readAge = memoryAge(fact.readAt, now, deviceTimeZone.offsetSecondsAt(now)),
        readTime = localTime(fact.readAt, deviceTimeZone.offsetSecondsAt(fact.readAt)),
        until = fact.expiresAt?.let { localDay(it, deviceTimeZone.offsetSecondsAt(it)) },
    )
}

/** The facts still to answer: open, saving or failed, but not done. */
fun List<HeldRow>.openCount(): Int = count { it.status !is HeldStatus.Done }

/**
 * Save, Don't save and Undo on facts not saved yet, shared by Changes to check and the chat sheet,
 * as [ChangeAnswers] is for changes. Only an open card takes a tap. A fact the hub no longer holds
 * open (expired, or answered on another phone) calls [onGone], which reads the list again.
 */
class HeldAnswers(
    private val scope: CoroutineScope,
    private val answerHeldFact: AnswerHeldFactUseCase,
    private val undoHeldFactAnswer: UndoHeldFactAnswerUseCase,
    private val rows: () -> List<HeldRow>,
    private val update: ((List<HeldRow>) -> List<HeldRow>) -> Unit,
    private val onGone: () -> Unit,
) {
    fun answer(
        heldId: String,
        answer: HeldFactAnswer,
    ) {
        val row = rows().firstOrNull { it.fact.id == heldId } ?: return
        if (row.status !is HeldStatus.Open) return
        set(heldId, HeldStatus.Saving(answer))
        scope.launch {
            runCatchingSafe { answerHeldFact(heldId, answer) }.fold(
                onSuccess = { set(heldId, HeldStatus.Done(answer)) },
                onFailure = { error ->
                    if (error is HeldFactGoneException) onGone() else set(heldId, HeldStatus.Open(failed = true))
                },
            )
        }
    }

    fun undo(heldId: String) {
        val done = rows().firstOrNull { it.fact.id == heldId }?.status as? HeldStatus.Done ?: return
        set(heldId, HeldStatus.Open())
        scope.launch {
            runCatchingSafe { undoHeldFactAnswer(heldId) }.onFailure {
                set(heldId, done.copy(undoFailed = true))
            }
        }
    }

    private fun set(
        heldId: String,
        status: HeldStatus,
    ) = update { rows -> rows.map { if (it.fact.id == heldId) it.copy(status = status) else it } }
}
