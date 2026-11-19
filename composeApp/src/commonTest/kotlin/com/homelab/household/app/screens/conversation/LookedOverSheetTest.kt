package com.homelab.household.app.screens.conversation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.homelab.household.domain.model.HeldFactAnswer
import com.homelab.household.domain.model.ProposalAnswer
import kotlin.test.Test
import kotlin.test.assertEquals

/** "Remembered from this chat": the sheet the quiet line opens. */
@OptIn(ExperimentalTestApi::class)
class LookedOverSheetTest {
    private val sheet = LookedOverUiStateProvider().values.first()

    @Test
    fun `GIVEN a looked-over chat WHEN the sheet opens THEN it shows what to check what waits for Liam and what was remembered`() =
        runComposeUiTest {
            lookedOverSheetContent(sheet)

            onLookedOverSheet {
                seesTheTitle(time = "10:52")
                seesToCheck(count = 1)
                sees("Eats fish again since September.")
                seesNoChatNamed("Dinner ideas")
                seesWaitingFor("Liam")
                seesRemembered(count = 2)
                sees("Studio Jury on Friday 17 October, 09:00.")
            }
        }

    @Test
    fun `GIVEN the sheet WHEN Use new X and All memories are tapped THEN each reaches its callback`() =
        runComposeUiTest {
            val answers = mutableListOf<Pair<String, ProposalAnswer>>()
            val removed = mutableListOf<String>()
            var allMemories = 0
            lookedOverSheetContent(
                sheet,
                onAnswer = { id, answer -> answers += id to answer },
                onRemove = { removed += it },
                onAllMemories = { allMemories++ },
            )

            onLookedOverSheet {
                usesNew()
                removes("Studio Jury on Friday 17 October, 09:00.")
                opensAllMemories()
            }

            assertEquals(listOf("c1" to ProposalAnswer.UseNew), answers)
            assertEquals(listOf("r1"), removed)
            assertEquals(1, allMemories)
        }

    private val withHeld = LookedOverUiStateProvider().values.first { it.held.isNotEmpty() }
    private val dentist = "Sam has a dentist appointment on Monday 23 November at 09:00."

    @Test
    fun `GIVEN a chat with a fact not saved yet WHEN the sheet opens THEN that fact comes first without naming the chat`() =
        runComposeUiTest {
            lookedOverSheetContent(withHeld)

            onLookedOverSheet {
                seesNotSavedYet(count = 1)
                sees(dentist)
                seesNotSavedYetBeforeToCheck()
                seesNoChatNamed("Weekend plans")
            }
        }

    @Test
    fun `GIVEN a fact not saved yet in the sheet WHEN Save is tapped THEN it reaches its callback`() =
        runComposeUiTest {
            val answers = mutableListOf<Pair<String, HeldFactAnswer>>()
            lookedOverSheetContent(withHeld, onHeldAnswer = { id, answer -> answers += id to answer })

            onLookedOverSheet { saves() }

            assertEquals(listOf("h1" to HeldFactAnswer.Save), answers)
        }

    @Test
    fun `GIVEN a fact not saved yet in the sheet WHEN Don't save is tapped THEN it reaches its callback`() =
        runComposeUiTest {
            val answers = mutableListOf<Pair<String, HeldFactAnswer>>()
            lookedOverSheetContent(withHeld, onHeldAnswer = { id, answer -> answers += id to answer })

            onLookedOverSheet { doesNotSave() }

            assertEquals(listOf("h1" to HeldFactAnswer.DontSave), answers)
        }

    @Test
    fun `GIVEN a sheet taller than the screen WHEN it draws THEN every row is laid out at once so its height never shifts`() =
        runComposeUiTest {
            lookedOverSheetContent(sheet, height = 300.dp)

            onLookedOverSheet { laysOutEveryRowAtOnce() }
        }

    @Test
    fun `GIVEN every previewed state WHEN it is drawn THEN it settles without failing`() {
        LookedOverUiStateProvider().values.forEach { state ->
            runComposeUiTest {
                lookedOverSheetContent(state)
                waitForIdle()
            }
        }
    }
}
