package com.homelab.household.app.screens.secretunlock

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEventBus
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.a11y_secret_on
import com.homelab.household.app.resources.conversation_back
import com.homelab.household.app.resources.conversation_greeting
import com.homelab.household.app.resources.pin_wrong
import com.homelab.household.app.resources.secret_locked_title
import com.homelab.household.app.resources.secret_marker_ended
import com.homelab.household.app.resources.secret_marker_from_here
import com.homelab.household.app.resources.secret_off_confirm
import com.homelab.household.app.resources.secret_part_unlock
import com.homelab.household.app.screens.conversation.ConversationScreen
import com.homelab.household.app.testing.FakeSecretChatHub
import com.homelab.household.app.testing.TestApp
import com.homelab.household.data.datasource.local.InMemorySessionStorage
import org.jetbrains.compose.resources.getString

/** A secret chat opened while locked: the PIN pad it shows, and what the right PIN opens. */
@OptIn(ExperimentalTestApi::class)
class SecretChatRobot(
    private val test: ComposeUiTest,
) {
    suspend fun seesItLocked() = seesText(getString(Res.string.secret_locked_title))

    /** Locked is never the new-chat screen, and there is nothing to type into. */
    suspend fun seesNoGreetingAndNoComposer() {
        test.onNodeWithText(getString(Res.string.conversation_greeting, "Emma Larsson")).assertDoesNotExist()
        test.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    fun typesThePin(pin: String) {
        pin.forEach { digit -> test.onNodeWithText(digit.toString()).performClick() }
        test.waitForIdle()
    }

    suspend fun seesTheWrongPin() = seesText(getString(Res.string.pin_wrong))

    fun seesTheSecretWords() = seesText(FakeSecretChatHub.SECRET_WORDS)

    fun seesNoSecretWords() {
        test.onNodeWithText(FakeSecretChatHub.SECRET_WORDS).assertDoesNotExist()
    }

    /** The turned-off chat reads around its secret part: what was asked before and after it. */
    fun seesTheOrdinaryPartsOfTheChat() {
        seesText(FakeSecretChatHub.ASKED_BEFORE)
        seesText(FakeSecretChatHub.ASKED_AFTER)
    }

    /** The secret part, folded into one band with its Unlock. */
    suspend fun seesTheLockedPart() = seesText(getString(Res.string.secret_part_unlock))

    suspend fun unlocksThePart() {
        test.onNodeWithText(getString(Res.string.secret_part_unlock)).performClick()
        test.waitForIdle()
    }

    /** Back on the PIN pad's own bar. */
    suspend fun goesBack() {
        test.onNode(hasContentDescription(getString(Res.string.conversation_back))).performClick()
        test.waitForIdle()
    }

    /** The part opened in place: its words between "Secret from here on" and "No longer secret from here". */
    suspend fun seesThePartOpenInPlace() {
        seesTheSecretWords()
        seesText(FakeSecretChatHub.SECRET_ANSWER)
        seesText(getString(Res.string.secret_marker_from_here))
        seesText(getString(Res.string.secret_marker_ended))
        seesTheOrdinaryPartsOfTheChat()
    }

    suspend fun turnsSecretModeOff() {
        test.onNode(hasContentDescription(getString(Res.string.a11y_secret_on))).performClick()
        test.waitUntilExactlyOneExists(hasText(getString(Res.string.secret_off_confirm)), timeoutMillis = WAIT_MILLIS)
        test.onNodeWithText(getString(Res.string.secret_off_confirm)).performClick()
        test.waitForIdle()
    }

    suspend fun seesItNoLongerSecret() = seesText(getString(Res.string.secret_marker_ended))

    private fun seesText(text: String) {
        test.waitUntilExactlyOneExists(hasText(text), timeoutMillis = WAIT_MILLIS)
    }

    private companion object {
        const val WAIT_MILLIS = 5_000L
    }
}

@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.onSecretChat(block: suspend SecretChatRobot.() -> Unit) {
    SecretChatRobot(this).block()
}

/** The real conversation screen and everything behind it, against [hub], opened on [sessionId]. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.secretChatScreen(
    hub: FakeSecretChatHub,
    sessionId: String = "s-1",
) {
    setContent {
        TestApp(hub.engine, sessionStorage = InMemorySessionStorage().apply { saveTokens("signed-in-token") }) {
            // Navigation provides the bus a conversation hears "calendar connected" on.
            CompositionLocalProvider(LocalResultEventBus provides ResultEventBus()) {
                ConversationScreen(sessionId = sessionId, onBack = {})
            }
        }
    }
}
