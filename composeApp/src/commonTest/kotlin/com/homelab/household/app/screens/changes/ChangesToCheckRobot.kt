package com.homelab.household.app.screens.changes

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.changes_done_replaced
import com.homelab.household.app.resources.changes_failed
import com.homelab.household.app.resources.changes_intro
import com.homelab.household.app.resources.changes_intro_all
import com.homelab.household.app.resources.changes_keep_both
import com.homelab.household.app.resources.changes_keep_stored
import com.homelab.household.app.resources.changes_none_title
import com.homelab.household.app.resources.changes_read
import com.homelab.household.app.resources.changes_saving
import com.homelab.household.app.resources.changes_section_changes
import com.homelab.household.app.resources.changes_section_held
import com.homelab.household.app.resources.changes_source_owner
import com.homelab.household.app.resources.changes_source_title
import com.homelab.household.app.resources.changes_stored
import com.homelab.household.app.resources.changes_title
import com.homelab.household.app.resources.changes_use_new
import com.homelab.household.app.resources.changes_waiting_for
import com.homelab.household.app.resources.changes_waiting_line
import com.homelab.household.app.resources.held_dont_save
import com.homelab.household.app.resources.held_done_not_saved
import com.homelab.household.app.resources.held_done_saved
import com.homelab.household.app.resources.held_household
import com.homelab.household.app.resources.held_label
import com.homelab.household.app.resources.held_rule
import com.homelab.household.app.resources.held_save
import com.homelab.household.app.resources.memories_offline_title
import com.homelab.household.app.resources.memories_tab_household_plain
import com.homelab.household.app.resources.memories_tab_yours_plain
import com.homelab.household.app.resources.memories_try_again
import com.homelab.household.app.resources.memories_undo
import com.homelab.household.app.resources.memories_until
import com.homelab.household.app.resources.to_check_all
import com.homelab.household.app.resources.tool_card_month_dec
import com.homelab.household.app.testing.FakeMembersHub
import com.homelab.household.app.testing.StillTheme
import com.homelab.household.app.testing.TestApp
import com.homelab.household.data.datasource.local.InMemorySessionStorage
import com.homelab.household.presentation.memories.ChangesToCheckUiState
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Everything Changes to check says and does, named as the resources it reads. */
@OptIn(ExperimentalTestApi::class)
class ChangesToCheckRobot(
    private val test: ComposeUiTest,
) {
    suspend fun seesTheTitle(count: Int) = seesText(getPluralString(Res.plurals.changes_title, count, count))

    suspend fun seesNothingLeft() = seesText(getString(Res.string.changes_none_title))

    /** Today's intro, when only changes wait. */
    suspend fun seesTheChangesIntro() = seesText(getString(Res.string.changes_intro))

    /** "2 to check" over facts and changes alike, with the intro that names both. */
    suspend fun seesTheHeadingForAll(count: Int) {
        seesText(getPluralString(Res.plurals.to_check_all, count, count))
        seesText(getString(Res.string.changes_intro_all))
    }

    /** "Not saved yet · 1" sits above "Changes · 1". */
    suspend fun seesFactsNotSavedYetBeforeChanges(
        held: Int,
        changes: Int,
    ) {
        val heldLabel = getString(Res.string.changes_section_held, held).uppercase()
        val changesLabel = getString(Res.string.changes_section_changes, changes).uppercase()
        seesInList(heldLabel)
        seesInList(changesLabel)
        val heldTop = test.onNodeWithText(heldLabel).getUnclippedBoundsInRoot().top
        val changesTop = test.onNodeWithText(changesLabel).getUnclippedBoundsInRoot().top
        assertTrue(heldTop < changesTop, "Not saved yet should come before Changes")
    }

    suspend fun seesNoSectionLabels() {
        doesNotSeeTheChangesLabel()
        assertNone(getString(Res.string.changes_section_held, 1).uppercase())
    }

    suspend fun doesNotSeeTheChangesLabel() {
        assertNone(getString(Res.string.changes_section_changes, 0).uppercase())
        assertNone(getString(Res.string.changes_section_changes, 1).uppercase())
    }

    private fun assertNone(text: String) = assertEquals(0, test.onAllNodes(hasText(text)).fetchSemanticsNodes().size)

    /** A fact's card: the label, the sentence, the rule line and Don't save · Save. */
    suspend fun seesAFactNotSavedYet(sentence: String) {
        seesInList(sentence)
        seesInList(getString(Res.string.held_label).uppercase(), atLeastOne = true)
        seesInList(getString(Res.string.held_rule), atLeastOne = true)
        seesInList(getString(Res.string.held_dont_save), atLeastOne = true)
        seesInList(getString(Res.string.held_save), atLeastOne = true)
    }

    suspend fun seesItIsYours() = seesInList(getString(Res.string.memories_tab_yours_plain), atLeastOne = true)

    suspend fun seesItIsTheHousehold() =
        seesInList(getString(Res.string.memories_tab_household_plain), atLeastOne = true)

    suspend fun seesItGoesInDecemberOn(day: Int) =
        seesInList(getString(Res.string.memories_until, day, getString(Res.string.tool_card_month_dec)))

    suspend fun seesSavingSharesIt() = seesInList(getString(Res.string.held_household))

    suspend fun seesNothingAboutSharing() = doesNotSee(getString(Res.string.held_household))

    suspend fun saves() = tapsText(getString(Res.string.held_save))

    suspend fun doesNotSave() = tapsText(getString(Res.string.held_dont_save))

    suspend fun seesItSaved(sentence: String) = seesInList(getString(Res.string.held_done_saved, sentence))

    suspend fun seesItNotSaved(sentence: String) = seesInList(getString(Res.string.held_done_not_saved, sentence))

    /** The stored sentence and the one read in the chat, each in its labelled box. */
    suspend fun seesTheChange(
        stored: String,
        read: String,
    ) {
        seesInList(stored)
        seesInList(read)
        seesInList(getString(Res.string.changes_stored).uppercase(), atLeastOne = true)
        seesInList(getString(Res.string.changes_read).uppercase(), atLeastOne = true)
    }

    suspend fun seesItWasReadIn(title: String) = seesSubstring(getString(Res.string.changes_source_title, title))

    suspend fun seesItWasReadInTheChatOf(owner: String) =
        seesSubstring(getString(Res.string.changes_source_owner, owner))

    suspend fun seesTheThreeAnswers() {
        seesInList(getString(Res.string.changes_keep_stored))
        seesInList(getString(Res.string.changes_keep_both))
        seesInList(getString(Res.string.changes_use_new))
    }

    suspend fun seesNoAnswers() {
        assertEquals(0, test.onAllNodes(hasText(getString(Res.string.changes_use_new))).fetchSemanticsNodes().size)
    }

    suspend fun usesNew() = tapsText(getString(Res.string.changes_use_new))

    suspend fun keepsStored() = tapsText(getString(Res.string.changes_keep_stored))

    suspend fun seesItSaving() = seesText(getString(Res.string.changes_saving))

    suspend fun seesItReplaced(stored: String) = seesInList(getString(Res.string.changes_done_replaced, stored))

    suspend fun undoes() = tapsText(getString(Res.string.memories_undo))

    /** The card after a late answer: the hub's own words, naming who took the sentence back. */
    fun seesItWasTakenBack(words: String) = seesInList(words)

    suspend fun seesTheAnswerFailed() = seesInList(getString(Res.string.changes_failed))

    suspend fun seesItWaitsFor(name: String) {
        seesInList(getString(Res.string.changes_waiting_for, name).uppercase())
        seesInList(getString(Res.string.changes_waiting_line, name))
    }

    suspend fun seesTheHubCannotBeReached() {
        seesText(getString(Res.string.memories_offline_title))
        seesText(getString(Res.string.memories_try_again))
    }

    fun doesNotSee(text: String) = test.waitUntilDoesNotExist(hasText(text), timeoutMillis = WAIT_MILLIS)

    private fun seesText(text: String) {
        test.waitUntilExactlyOneExists(hasText(text), timeoutMillis = WAIT_MILLIS)
        test.onNodeWithText(text).assertIsDisplayed()
    }

    private fun seesInList(
        text: String,
        atLeastOne: Boolean = false,
    ) {
        if (atLeastOne) {
            test.waitUntil(timeoutMillis = WAIT_MILLIS) {
                test.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
            }
            return
        }
        test.waitUntilExactlyOneExists(hasText(text), timeoutMillis = WAIT_MILLIS)
        test.onNodeWithText(text).performScrollTo().assertIsDisplayed()
    }

    private fun seesSubstring(text: String) {
        test.waitUntil(timeoutMillis = WAIT_MILLIS) {
            test.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
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
suspend fun ComposeUiTest.onChangesToCheck(block: suspend ChangesToCheckRobot.() -> Unit) {
    ChangesToCheckRobot(this).block()
}

/** Changes to check for a signed-in member, over a hub that answers however the test says. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.changesToCheckScreen(hub: FakeMembersHub) {
    setContent {
        TestApp(hub.engine, sessionStorage = InMemorySessionStorage().apply { saveTokens("signed-in-token") }) {
            ChangesToCheckScreen(onBack = {})
        }
    }
}

/** Changes to check drawn from [state] alone. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.changesToCheckContent(state: ChangesToCheckUiState) {
    setContent {
        StillTheme {
            ChangesToCheckContent(state = state, onBack = {}, onAnswer = { _, _ -> }, onUndo = {}, onRetry = {})
        }
    }
}
