package com.homelab.household.presentation.memories

import com.homelab.household.domain.exception.ChangeWithdrawnException
import com.homelab.household.domain.exception.HeldFactGoneException
import com.homelab.household.domain.exception.ServerOfflineException
import com.homelab.household.domain.model.ChangeCard
import com.homelab.household.domain.model.ChangesToCheck
import com.homelab.household.domain.model.HeldFact
import com.homelab.household.domain.model.HeldFactAnswer
import com.homelab.household.domain.model.MemoryItem
import com.homelab.household.domain.model.MemoryOrigin
import com.homelab.household.domain.model.MemoryScope
import com.homelab.household.domain.model.ProposalAnswer
import com.homelab.household.domain.usecase.AnswerChangeUseCase
import com.homelab.household.domain.usecase.AnswerHeldFactUseCase
import com.homelab.household.domain.usecase.GetChangesToCheckUseCase
import com.homelab.household.domain.usecase.UndoChangeAnswerUseCase
import com.homelab.household.domain.usecase.UndoHeldFactAnswerUseCase
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Changes to check: the cards you answer, the ones waiting for someone else, and Undo in place. */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class ChangesToCheckViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val getChanges = mock<GetChangesToCheckUseCase>()
    private val answerChange = mock<AnswerChangeUseCase>()
    private val undoAnswer = mock<UndoChangeAnswerUseCase>()
    private val answerHeld = mock<AnswerHeldFactUseCase>()
    private val undoHeld = mock<UndoHeldFactAnswerUseCase>()
    private val now = Instant.parse("2026-10-16T18:00:00Z")
    private val clock =
        object : Clock {
            override fun now() = now
        }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val changes =
        ChangesToCheck(
            toCheck = listOf(card("p-sam"), card("p-office")),
            waiting = listOf(card("p-boiler", canAnswer = false, answerer = "Liam")),
        )

    private fun viewModel() =
        ChangesToCheckViewModel(
            getChangesToCheck = getChanges,
            answerChange = answerChange,
            undoChangeAnswer = undoAnswer,
            answerHeldFact = answerHeld,
            undoHeldFactAnswer = undoHeld,
            deviceTimeZone = { 0 },
            clock = clock,
        )

    private val withHeld = changes.copy(held = listOf(heldFact("h-dentist")))

    private fun ChangesToCheckUiState.heldRow(id: String) = held.first { it.fact.id == id }

    private fun ChangesToCheckUiState.row(id: String) = (toCheck + waiting).first { it.card.id == id }

    @Test
    fun `GIVEN changes on the hub WHEN the page opens THEN yours and the waiting ones load with the count`() =
        runTest(testDispatcher) {
            everySuspend { getChanges() } returns changes

            val viewModel = viewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(MemoriesStatus.Ready, state.status)
            assertEquals(2, state.openCount)
            assertEquals(listOf("p-sam", "p-office"), state.toCheck.map { it.card.id })
            assertEquals(
                listOf("Liam" to listOf("p-boiler")),
                state.waitingByAnswerer.map { (who, rows) ->
                    who to
                        rows.map { it.card.id }
                },
            )
            assertEquals(MemoryAge.Today, state.row("p-sam").readAge)
            assertEquals(LocalTime(hour = 10, minute = 42), state.row("p-sam").readTime)
            assertEquals(MemoryAge.Month(3), state.row("p-sam").storedAge)
            assertEquals(ChangeStatus.Open(), state.row("p-sam").status)
        }

    @Test
    fun `GIVEN the hub can't be reached WHEN the page opens THEN it says so and Try again loads it`() =
        runTest(testDispatcher) {
            everySuspend { getChanges() } throws ServerOfflineException("offline")
            val viewModel = viewModel()
            advanceUntilIdle()
            assertEquals(MemoriesStatus.Unreachable, viewModel.uiState.value.status)

            everySuspend { getChanges() } returns changes
            viewModel.load()
            advanceUntilIdle()

            assertEquals(MemoriesStatus.Ready, viewModel.uiState.value.status)
        }

    @Test
    fun `GIVEN a change WHEN Use new is tapped THEN it saves then becomes a done row and stops counting`() =
        runTest(testDispatcher) {
            everySuspend { getChanges() } returns changes
            val gate = CompletableDeferred<Unit>()
            everySuspend { answerChange("p-sam", ProposalAnswer.UseNew) } calls { gate.await() }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onAnswer("p-sam", ProposalAnswer.UseNew)
            runCurrent()
            assertEquals(ChangeStatus.Saving(ProposalAnswer.UseNew), viewModel.uiState.value.row("p-sam").status)
            viewModel.onAnswer("p-sam", ProposalAnswer.KeepStored) // a second tap while saving does nothing
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(ChangeStatus.Done(ProposalAnswer.UseNew), viewModel.uiState.value.row("p-sam").status)
            assertEquals(1, viewModel.uiState.value.openCount)
            verifySuspend(VerifyMode.exactly(0)) { answerChange("p-sam", ProposalAnswer.KeepStored) }
        }

    @Test
    fun `GIVEN a change WHEN the answer doesn't save THEN the card is open again and says so`() =
        runTest(testDispatcher) {
            everySuspend { getChanges() } returns changes
            everySuspend { answerChange(any(), any()) } throws ServerOfflineException("offline")
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onAnswer("p-sam", ProposalAnswer.KeepBoth)
            advanceUntilIdle()

            assertEquals(ChangeStatus.Open(failed = true), viewModel.uiState.value.row("p-sam").status)
            assertEquals(2, viewModel.uiState.value.openCount)
        }

    @Test
    fun `GIVEN someone else's change WHEN an answer is tapped THEN nothing is sent`() =
        runTest(testDispatcher) {
            everySuspend { getChanges() } returns changes
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onAnswer("p-boiler", ProposalAnswer.UseNew)
            advanceUntilIdle()

            verifySuspend(VerifyMode.exactly(0)) { answerChange(any(), any()) }
            assertEquals(ChangeStatus.Open(), viewModel.uiState.value.row("p-boiler").status)
        }

    @Test
    fun `GIVEN an answered change WHEN Undo is tapped THEN the card is open again`() =
        runTest(testDispatcher) {
            everySuspend { getChanges() } returns changes
            everySuspend { answerChange(any(), any()) } returns Unit
            everySuspend { undoAnswer("p-sam") } returns Unit
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onAnswer("p-sam", ProposalAnswer.KeepStored)
            advanceUntilIdle()

            viewModel.onUndo("p-sam")
            advanceUntilIdle()

            assertEquals(ChangeStatus.Open(), viewModel.uiState.value.row("p-sam").status)
            assertEquals(2, viewModel.uiState.value.openCount)
            verifySuspend(VerifyMode.exactly(1)) { undoAnswer("p-sam") }
        }

    @Test
    fun `GIVEN an answered change WHEN Undo doesn't save THEN the done row stays and says so`() =
        runTest(testDispatcher) {
            everySuspend { getChanges() } returns changes
            everySuspend { answerChange(any(), any()) } returns Unit
            everySuspend { undoAnswer(any()) } throws ServerOfflineException("offline")
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onAnswer("p-sam", ProposalAnswer.UseNew)
            advanceUntilIdle()

            viewModel.onUndo("p-sam")
            advanceUntilIdle()

            assertEquals(
                ChangeStatus.Done(ProposalAnswer.UseNew, undoFailed = true),
                viewModel.uiState.value.row("p-sam").status,
            )
        }

    @Test
    fun `GIVEN every change answered WHEN the page shows THEN nothing is left to check`() =
        runTest(testDispatcher) {
            everySuspend { getChanges() } returns changes
            everySuspend { answerChange(any(), any()) } returns Unit
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onAnswer("p-sam", ProposalAnswer.UseNew)
            viewModel.onAnswer("p-office", ProposalAnswer.KeepStored)
            advanceUntilIdle()

            assertEquals(0, viewModel.uiState.value.openCount)
        }

    companion object {
        @OptIn(ExperimentalTime::class)
        fun card(
            id: String,
            canAnswer: Boolean = true,
            answerer: String = "Emma",
        ) = ChangeCard(
            id = id,
            content = "New sentence for $id",
            stored =
                MemoryItem(
                    id = "stored-$id",
                    content = "Stored sentence for $id",
                    scope = MemoryScope.HOUSEHOLD,
                    origin = MemoryOrigin.Reflection,
                    createdAt = Instant.parse("2026-03-01T09:00:00Z"),
                    expiresAt = null,
                    speakerId = if (canAnswer) "emma" else "liam",
                    speakerName = answerer,
                    isMine = canAnswer,
                    agentName = "Planner",
                    agentAvatar = "🗓️",
                    canRemove = canAnswer,
                ),
            canAnswer = canAnswer,
            answererName = answerer,
            sourceTitle = "Dinner ideas",
            sourceOwnerName = "Emma",
            readAt = Instant.parse("2026-10-16T10:42:00Z"),
        )

        @OptIn(ExperimentalTime::class)
        fun heldFact(
            id: String,
            household: Boolean = false,
            expiresAt: Instant? = Instant.parse("2026-10-23T10:00:00Z"),
        ) = HeldFact(
            id = id,
            content = "Sam has a dentist appointment on Monday at 09:00.",
            scope = if (household) MemoryScope.HOUSEHOLD else MemoryScope.PERSONAL,
            agentName = "Planner",
            createdAt = Instant.parse("2026-10-16T10:42:00Z"),
            expiresAt = expiresAt,
            sourceTitle = "Weekend plans",
            sourceOwnerName = "Emma",
            readAt = Instant.parse("2026-10-16T10:40:00Z"),
        )
    }

    @Test
    fun `GIVEN Emma took her sentence back WHEN Liam answers it THEN the card says so and no longer counts`() =
        runTest(testDispatcher) {
            everySuspend { getChanges() } returns changes
            everySuspend { answerChange("p-sam", ProposalAnswer.UseNew) } throws
                ChangeWithdrawnException("Emma took this back.")
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onAnswer("p-sam", ProposalAnswer.UseNew)
            advanceUntilIdle()

            assertEquals(ChangeStatus.Withdrawn("Emma took this back."), viewModel.uiState.value.row("p-sam").status)
            assertEquals(1, viewModel.uiState.value.openCount)
        }

    @Test
    fun `GIVEN a fact not saved yet WHEN the page opens THEN it loads with when it was read and its end day`() =
        runTest(testDispatcher) {
            everySuspend { getChanges() } returns withHeld

            val viewModel = viewModel()
            advanceUntilIdle()

            val row = viewModel.uiState.value.heldRow("h-dentist")
            assertEquals(MemoryAge.Today, row.readAge)
            assertEquals(LocalTime(hour = 10, minute = 40), row.readTime)
            assertEquals(LocalDay(day = 23, month = 10), row.until)
            assertEquals(HeldStatus.Open(), row.status)
        }

    @Test
    fun `GIVEN changes and a fact not saved yet WHEN the page opens THEN the count takes in both`() =
        runTest(testDispatcher) {
            everySuspend { getChanges() } returns withHeld

            val viewModel = viewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(3, state.openCount)
            assertEquals(1, state.heldOpenCount)
            assertEquals(2, state.changesOpenCount)
        }

    @Test
    fun `GIVEN a fact not saved yet WHEN Save is tapped THEN it saves then becomes a done row and stops counting`() =
        runTest(testDispatcher) {
            everySuspend { getChanges() } returns withHeld
            val gate = CompletableDeferred<Unit>()
            everySuspend { answerHeld("h-dentist", HeldFactAnswer.Save) } calls { gate.await() }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onHeldAnswer("h-dentist", HeldFactAnswer.Save)
            runCurrent()
            assertEquals(HeldStatus.Saving(HeldFactAnswer.Save), viewModel.uiState.value.heldRow("h-dentist").status)
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(HeldStatus.Done(HeldFactAnswer.Save), viewModel.uiState.value.heldRow("h-dentist").status)
            assertEquals(0, viewModel.uiState.value.heldOpenCount)
        }

    @Test
    fun `GIVEN a fact not saved yet WHEN the answer doesn't reach the hub THEN the card is open again and says so`() =
        runTest(testDispatcher) {
            everySuspend { getChanges() } returns withHeld
            everySuspend { answerHeld(any(), any()) } throws ServerOfflineException("offline")
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onHeldAnswer("h-dentist", HeldFactAnswer.DontSave)
            advanceUntilIdle()

            assertEquals(HeldStatus.Open(failed = true), viewModel.uiState.value.heldRow("h-dentist").status)
            assertEquals(1, viewModel.uiState.value.heldOpenCount)
        }

    @Test
    fun `GIVEN a fact answered on another phone WHEN it is answered here THEN the page is read again without it`() =
        runTest(testDispatcher) {
            everySuspend { getChanges() } returns withHeld
            everySuspend { answerHeld(any(), any()) } calls {
                everySuspend { getChanges() } returns changes
                throw HeldFactGoneException()
            }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onHeldAnswer("h-dentist", HeldFactAnswer.Save)
            advanceUntilIdle()

            assertEquals(emptyList(), viewModel.uiState.value.held)
            assertEquals(MemoriesStatus.Ready, viewModel.uiState.value.status)
            verifySuspend(VerifyMode.exactly(2)) { getChanges() }
        }

    @Test
    fun `GIVEN a saved fact WHEN Undo is tapped THEN the card is open again`() =
        runTest(testDispatcher) {
            everySuspend { getChanges() } returns withHeld
            everySuspend { answerHeld(any(), any()) } returns Unit
            everySuspend { undoHeld("h-dentist") } returns Unit
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onHeldAnswer("h-dentist", HeldFactAnswer.Save)
            advanceUntilIdle()

            viewModel.onHeldUndo("h-dentist")
            advanceUntilIdle()

            assertEquals(HeldStatus.Open(), viewModel.uiState.value.heldRow("h-dentist").status)
            verifySuspend(VerifyMode.exactly(1)) { undoHeld("h-dentist") }
        }

    @Test
    fun `GIVEN a fact not saved on purpose WHEN Undo doesn't reach the hub THEN the done row stays and says so`() =
        runTest(testDispatcher) {
            everySuspend { getChanges() } returns withHeld
            everySuspend { answerHeld(any(), any()) } returns Unit
            everySuspend { undoHeld(any()) } throws ServerOfflineException("offline")
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onHeldAnswer("h-dentist", HeldFactAnswer.DontSave)
            advanceUntilIdle()

            viewModel.onHeldUndo("h-dentist")
            advanceUntilIdle()

            assertEquals(
                HeldStatus.Done(HeldFactAnswer.DontSave, undoFailed = true),
                viewModel.uiState.value.heldRow("h-dentist").status,
            )
        }
}
