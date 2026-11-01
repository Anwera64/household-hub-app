package com.homelab.household.app.screens.secretunlock

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEventBus
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.conversation_greeting
import com.homelab.household.app.resources.pin_wrong
import com.homelab.household.app.resources.secret_locked_title
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

/** The real conversation screen and everything behind it, against [hub], opened on its secret chat. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.secretChatScreen(hub: FakeSecretChatHub) {
    setContent {
        TestApp(hub.engine, sessionStorage = InMemorySessionStorage().apply { saveTokens("signed-in-token") }) {
            // Navigation provides the bus a conversation hears "calendar connected" on.
            CompositionLocalProvider(LocalResultEventBus provides ResultEventBus()) {
                ConversationScreen(sessionId = "s-1", onBack = {})
            }
        }
    }
}
