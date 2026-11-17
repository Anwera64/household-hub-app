package com.homelab.household.app.screens.conversation

import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.unit.Dp
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.changes_section_held
import com.homelab.household.app.resources.changes_source_title
import com.homelab.household.app.resources.changes_use_new
import com.homelab.household.app.resources.changes_waiting_for
import com.homelab.household.app.resources.held_dont_save
import com.homelab.household.app.resources.held_save
import com.homelab.household.app.resources.looked_over_all_memories
import com.homelab.household.app.resources.looked_over_caption
import com.homelab.household.app.resources.looked_over_remembered
import com.homelab.household.app.resources.looked_over_title
import com.homelab.household.app.resources.looked_over_to_check
import com.homelab.household.app.resources.memories_remove
import com.homelab.household.app.testing.StillTheme
import com.homelab.household.domain.model.HeldFactAnswer
import com.homelab.household.domain.model.ProposalAnswer
import com.homelab.household.presentation.memories.LookedOverUiState
import org.jetbrains.compose.resources.getString
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Everything "Remembered from this chat" says and does, named as the resources it reads. */
@OptIn(ExperimentalTestApi::class)
class LookedOverSheetRobot(
    private val test: ComposeUiTest,
) {
    suspend fun seesTheTitle(time: String) {
        seesText(getString(Res.string.looked_over_title))
        seesText(getString(Res.string.looked_over_caption, time))
    }

    suspend fun seesToCheck(count: Int) = seesText(getString(Res.string.looked_over_to_check, count).uppercase())

    suspend fun seesNotSavedYet(count: Int) =
        seesText(getString(Res.string.changes_section_held, count).uppercase())

    /** "Not saved yet · 1" sits above "To check · 1". */
    suspend fun seesNotSavedYetBeforeToCheck() {
        val held = getString(Res.string.changes_section_held, 1).uppercase()
        val toCheck = getString(Res.string.looked_over_to_check, 1).uppercase()
        seesText(held)
        seesText(toCheck)
        val heldTop = test.onNodeWithText(held).getUnclippedBoundsInRoot().top
        val toCheckTop = test.onNodeWithText(toCheck).getUnclippedBoundsInRoot().top
        assertTrue(heldTop < toCheckTop, "Not saved yet should come before To check")
    }

    suspend fun saves() = tapsText(getString(Res.string.held_save))

    suspend fun doesNotSave() = tapsText(getString(Res.string.held_dont_save))

    suspend fun seesWaitingFor(name: String) = seesText(getString(Res.string.changes_waiting_for, name).uppercase())

    suspend fun seesRemembered(count: Int) = seesText(getString(Res.string.looked_over_remembered, count).uppercase())

    fun sees(words: String) = seesText(words)

    /** The chat is the one already open, so cards don't name it. */
    suspend fun seesNoChatNamed(title: String) {
        val named = getString(Res.string.changes_source_title, title)
        assertEquals(0, test.onAllNodes(hasText(named, substring = true)).fetchSemanticsNodes().size)
    }

    suspend fun usesNew() = tapsText(getString(Res.string.changes_use_new))

    suspend fun removes(content: String) {
        val button = hasContentDescription(getString(Res.string.memories_remove, content))
        test.waitUntilExactlyOneExists(button, timeoutMillis = WAIT_MILLIS)
        test.onNode(button).performScrollTo().performClick()
    }

    suspend fun opensAllMemories() = tapsText(getString(Res.string.looked_over_all_memories))

    /** Its last row is laid out before any scrolling: the sheet measures all it holds at once. */
    suspend fun laysOutEveryRowAtOnce() {
        test.waitForIdle()
        val link = getString(Res.string.looked_over_all_memories)
        assertEquals(1, test.onAllNodes(hasText(link)).fetchSemanticsNodes().size)
    }

    private fun seesText(text: String) {
        test.waitUntilExactlyOneExists(hasText(text), timeoutMillis = WAIT_MILLIS)
        test.onNodeWithText(text).performScrollTo().assertIsDisplayed()
    }

    private fun tapsText(text: String) {
        test.waitUntilExactlyOneExists(hasText(text), timeoutMillis = WAIT_MILLIS)
        test.onNodeWithText(text).performScrollTo().performClick()
    }

    private companion object {
        const val WAIT_MILLIS = 5_000L
    }
}

@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.onLookedOverSheet(block: suspend LookedOverSheetRobot.() -> Unit) {
    LookedOverSheetRobot(this).block()
}

/** The sheet's insides drawn from [state] alone, with only the callbacks a test cares about. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.lookedOverSheetContent(
    state: LookedOverUiState,
    onAnswer: (String, ProposalAnswer) -> Unit = { _, _ -> },
    onRemove: (String) -> Unit = {},
    onHeldAnswer: (String, HeldFactAnswer) -> Unit = { _, _ -> },
    onAllMemories: () -> Unit = {},
    height: Dp = Dp.Unspecified,
) {
    setContent {
        StillTheme {
            LookedOverSheetContent(
                state = state,
                onAnswer = onAnswer,
                onUndo = {},
                onRemove = onRemove,
                onRestore = {},
                onRetry = {},
                onAllMemories = onAllMemories,
                modifier = if (height == Dp.Unspecified) Modifier else Modifier.height(height),
                onHeldAnswer = onHeldAnswer,
            )
        }
    }
}
