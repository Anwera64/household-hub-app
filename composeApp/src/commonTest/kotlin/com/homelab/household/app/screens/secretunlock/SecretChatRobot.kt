package com.homelab.household.app.screens.secretunlock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEventBus
import com.homelab.household.app.navigation.AppNavHost
import com.homelab.household.app.navigation.AppScreens
import com.homelab.household.app.navigation.Destination
import com.homelab.household.app.navigation.RealAppScreens
import com.homelab.household.app.navigation.SecretChatsUnlocked
import com.homelab.household.app.navigation.StubScreens
import com.homelab.household.app.navigation.StubSession
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.a11y_secret_on
import com.homelab.household.app.resources.conversation_greeting
import com.homelab.household.app.resources.pin_entered
import com.homelab.household.app.resources.pin_unreachable
import com.homelab.household.app.resources.secret_chats_unlock_title
import com.homelab.household.app.resources.secret_locked_all_line
import com.homelab.household.app.resources.secret_locked_title
import com.homelab.household.app.resources.secret_marker_ended
import com.homelab.household.app.resources.secret_marker_from_here
import com.homelab.household.app.resources.secret_off_confirm
import com.homelab.household.app.resources.secret_part_unlock
import com.homelab.household.app.resources.send_message
import com.homelab.household.app.screens.conversation.ConversationScreen
import com.homelab.household.app.screens.pinentry.PIN_DOTS_TAG
import com.homelab.household.app.testing.FakeSecretChatHub
import com.homelab.household.app.testing.TestApp
import com.homelab.household.data.datasource.local.InMemorySessionStorage
import com.homelab.household.domain.secret.SecretLockController
import com.homelab.household.domain.usecase.UnlockSecretChatsUseCase
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform

/**
 * What the back stack gives a chat: the bus it hears results on, and how often it asked for the
 * secret chats PIN pad. The pad is a destination of its own, opened over the chat and never drawn
 * inside it: AppNavHostTest covers where it goes, and [secretChatInTheApp] the real one.
 */
class ChatNavigation {
    val results = ResultEventBus()

    var pinPadsAsked = 0

    /** Of those, how many were for a send held for the PIN. */
    var pinPadsAskedToSend = 0
}

/** A secret chat opened while locked: the locked screen or its bands, and what the right PIN opens. */
@OptIn(ExperimentalTestApi::class)
class SecretChatRobot(
    private val test: ComposeUiTest,
    private val navigation: ChatNavigation,
) {
    /** The locked screen: everything in the chat is secret, and Unlock is the way in. */
    suspend fun seesItLocked() {
        seesText(getString(Res.string.secret_locked_title))
        seesText(getString(Res.string.secret_locked_all_line))
        seesText(getString(Res.string.secret_part_unlock))
    }

    /** The message box, there to type into. */
    fun seesTheComposer() {
        test.waitUntilExactlyOneExists(hasSetTextAction(), timeoutMillis = WAIT_MILLIS)
    }

    /** Types [words] into the message box and sends them. */
    suspend fun says(words: String) {
        test.onNode(hasSetTextAction()).performTextInput(words)
        test.onNodeWithContentDescription(getString(Res.string.send_message)).performClick()
        test.waitForIdle()
    }

    /** [words] are still in the message box, unsent. */
    fun seesInTheBox(words: String) {
        test.waitUntilExactlyOneExists(hasSetTextAction() and hasText(words), timeoutMillis = WAIT_MILLIS)
    }

    /** What was said before Secret Mode went on, in a chat still secret. */
    fun seesTheOrdinaryMessagesBeforeSecretMode() {
        seesText(FakeSecretChatHub.ASKED_BEFORE)
        seesText(FakeSecretChatHub.ANSWERED_BEFORE)
    }

    /** Locked is never the new-chat screen, and there is nothing to type into. */
    suspend fun seesNoGreetingAndNoComposer() {
        test.onNodeWithText(getString(Res.string.conversation_greeting, "Emma Larsson")).assertDoesNotExist()
        test.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    fun typesThePin(pin: String) {
        pin.forEach { digit -> test.onNodeWithText(digit.toString()).performClick() }
        test.waitForIdle()
    }

    /** The pad's dots, by what they read out: [count] digits typed. */
    suspend fun seesDigitsEntered(count: Int) {
        test.waitUntilExactlyOneExists(
            hasContentDescription(getPluralString(Res.plurals.pin_entered, count, count)),
            timeoutMillis = WAIT_MILLIS,
        )
    }

    /** The pad takes digits: it isn't waiting on the hub with a PIN it already sent. */
    fun seesThePinNotBeingChecked() {
        test.waitUntilExactlyOneExists(hasTestTag(PIN_DOTS_TAG), timeoutMillis = WAIT_MILLIS)
        test.onNodeWithTag(PIN_DOTS_TAG).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    /** Lock now, on the bar over Chats; leaving the app does the same. */
    fun locksSecretChats() {
        test.runOnUiThread { KoinPlatform.getKoin().get<SecretLockController>().lockNow() }
        test.waitForIdle()
    }

    /** The same pad opened from the bar over Chats, worded for every secret chat. */
    suspend fun seesUnlockSecretChats() = seesText(getString(Res.string.secret_chats_unlock_title))

    suspend fun seesTheHubDidNotAnswer() = seesText(getString(Res.string.pin_unreachable))

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

    /** Unlock, on the locked screen or on a band. */
    suspend fun unlocks() {
        test.onNodeWithText(getString(Res.string.secret_part_unlock)).performClick()
        test.waitForIdle()
    }

    /** The PIN typed on the secret chats pad, which then tells the chat it opened them. */
    suspend fun opensSecretChatsOnThePad() {
        KoinPlatform.getKoin().get<UnlockSecretChatsUseCase>()(FakeSecretChatHub.RIGHT_PIN)
        test.runOnUiThread { navigation.results.sendResult(result = SecretChatsUnlocked(thenSend = false)) }
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
suspend fun ComposeUiTest.onSecretChat(
    navigation: ChatNavigation = ChatNavigation(),
    block: suspend SecretChatRobot.() -> Unit,
) {
    SecretChatRobot(this, navigation).block()
}

/** The real conversation screen and everything behind it, against [hub], opened on [sessionId]. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.secretChatScreen(
    hub: FakeSecretChatHub,
    sessionId: String = "s-1",
    navigation: ChatNavigation = ChatNavigation(),
) {
    setContent {
        TestApp(hub.engine, sessionStorage = InMemorySessionStorage().apply { saveTokens("signed-in-token") }) {
            // Navigation provides the bus a conversation hears "calendar connected" and the unlock on.
            CompositionLocalProvider(LocalResultEventBus provides navigation.results) {
                ConversationScreen(
                    sessionId = sessionId,
                    onBack = {},
                    onUnlockSecretChats = { thenSend ->
                        navigation.pinPadsAsked++
                        if (thenSend) navigation.pinPadsAskedToSend++
                    },
                )
            }
        }
    }
}

/**
 * A secret chat in the app's own navigation, opened from Chats: the real chat and the real secret
 * chats PIN pad over it, against [hub]. Every other screen is a stub.
 */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.secretChatInTheApp(
    hub: FakeSecretChatHub,
    sessionId: String = "s-1",
) {
    setContent {
        TestApp(hub.engine, sessionStorage = InMemorySessionStorage().apply { saveTokens("signed-in-token") }) {
            val backStack =
                remember { mutableStateListOf<NavKey>(Destination.Chats, Destination.Conversation(sessionId)) }
            AppNavHost(screens = SecretChatScreens(), backStack = backStack, session = StubSession())
        }
    }
}

/** The app's screens with only a chat and the secret chats PIN pad real. */
private class SecretChatScreens : AppScreens by StubScreens() {
    @Composable
    override fun SecretChatsUnlock(
        onUnlock: () -> Unit,
        onBack: () -> Unit,
    ) = RealAppScreens.SecretChatsUnlock(onUnlock = onUnlock, onBack = onBack)

    @Composable
    override fun Conversation(
        sessionId: String?,
        onBack: () -> Unit,
        onConnectCalendar: () -> Unit,
        onAllMemories: () -> Unit,
        onUnlockSecretChats: (thenSend: Boolean) -> Unit,
    ) = RealAppScreens.Conversation(sessionId, onBack, onConnectCalendar, onAllMemories, onUnlockSecretChats)
}
