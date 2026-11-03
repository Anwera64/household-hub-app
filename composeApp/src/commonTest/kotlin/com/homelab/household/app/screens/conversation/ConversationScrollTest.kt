package com.homelab.household.app.screens.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.homelab.household.app.testing.StillTheme
import com.homelab.household.domain.model.ChatMessage
import com.homelab.household.domain.model.ConversationSession
import com.homelab.household.domain.model.MessageRole
import com.homelab.household.presentation.chatsession.ChatSessionUiState
import com.homelab.household.presentation.chatsession.SecretPart
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test

/**
 * A long answer arrives faster than anyone reads it, and it arrives at the bottom.
 *
 * Without the list following it, the words appear below the fold and the screen sits still while
 * the agent talks — which looks, from the sofa, exactly like nothing happening. The same goes for
 * a chat you have been having for weeks: it has to open on the newest message, not the first.
 *
 * These hand the composition a `StandardTestDispatcher` deliberately. It queues coroutines rather
 * than running them eagerly, which is what the phone does. Under the default eager dispatcher the
 * scrolling `LaunchedEffect` gets to look at the list before it has been measured around the new
 * messages, every test here passes against a screen that follows nothing, and that is how this
 * shipped broken.
 */
@OptIn(ExperimentalTestApi::class)
class ConversationScrollTest {
    private val provider = ConversationUiStateProvider()

    private fun stateNamed(name: String): ChatSessionUiState {
        val index = (0 until provider.values.count()).first { provider.getDisplayName(it) == name }
        return provider.values.elementAt(index)
    }

    private fun conversation(state: ChatSessionUiState): @Composable () -> Unit = growing { state }

    /** The screen reading its state each recomposition, so a message can land while it is open. */
    private fun growing(state: () -> ChatSessionUiState): @Composable () -> Unit =
        {
            StillTheme {
                // A phone, so a long answer actually outgrows the viewport. The test window is
                // otherwise large enough to show everything, and would prove nothing.
                Box(modifier = Modifier.size(PHONE_WIDTH, PHONE_HEIGHT)) {
                    ConversationContent(
                        state = state(),
                        onComposerTextChange = {},
                        onSend = {},
                        onRetry = {},
                        onTryAgain = {},
                        onBack = {},
                        memberName = "Emma",
                    )
                }
            }
        }

    /**
     * A few more turns, and then the newest answer.
     *
     * More than one message, and more than a line each, because a list parked at its very end has
     * empty space below it: a single short answer lands in that space and is on screen whether or
     * not anything followed it, which is no test at all.
     */
    private fun ChatSessionUiState.andThen(newest: String) =
        copy(
            messages =
                messages +
                    (1..6).map {
                        ChatMessage(
                            id = "later-$it",
                            sessionId = "s-1",
                            role = if (it % 2 == 0) MessageRole.ASSISTANT else MessageRole.USER,
                            content = "Turn $it after the fold, with enough in it to fill a line or two of the screen.",
                        )
                    } +
                    ChatMessage(id = "newest", sessionId = "s-1", role = MessageRole.ASSISTANT, content = newest),
        )

    @Test
    fun `GIVEN a long answer arriving WHEN the conversation is shown THEN the question is scrolled off the top`() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            setContent(conversation(stateNamed("A long answer arriving")))
            waitForIdle()

            // The answer is one text node, so "is the newest sentence on screen" cannot tell
            // scrolled from not — the node counts as displayed the moment any of it is. What the
            // question does is the honest signal: following the answer pushes it off the top, far
            // enough that the list stops composing it at all.
            onConversationScroll { doesNotSee(QUESTION) }
        }

    @Test
    fun `GIVEN a long conversation whose transcript lands after the greeting WHEN the screen is open THEN it shows the newest message and not the oldest`() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            // As the screen really opens: the greeting is drawn first and the transcript lands a
            // moment later, so the list is composed by a recomposition, not by the first pass.
            val opened = stateNamed("A conversation opened again")
            var state by mutableStateOf(opened.copy(messages = emptyList()))
            setContent(growing { state })

            state = opened

            onConversationScroll {
                waitsForText(NEWEST_ANSWER)
                doesNotSee(OLDEST_QUESTION)
            }
        }

    @Test
    fun `GIVEN a long conversation at its newest message WHEN a newer answer lands THEN the transcript follows it down`() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            var state by mutableStateOf(stateNamed("A conversation opened again"))
            setContent(growing { state })
            onConversationScroll { waitsForText(NEWEST_ANSWER) }

            state = state.andThen(LATEST_ANSWER)

            onConversationScroll { seesText(LATEST_ANSWER) }
        }

    @Test
    fun `GIVEN a long conversation scrolled back to its start WHEN a newer answer lands THEN the oldest question stays and the answer is not scrolled to`() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            var state by mutableStateOf(stateNamed("A conversation opened again"))
            setContent(growing { state })
            onConversationScroll {
                waitsForText(NEWEST_ANSWER)
                scrollsBackToTheStart()
                waitsForText(OLDEST_QUESTION)
            }

            state = state.andThen(LATEST_ANSWER)
            waitForIdle()

            // Following unconditionally would make a long answer impossible to read back while it
            // is still being written.
            onConversationScroll {
                seesItStillShown(OLDEST_QUESTION)
                doesNotSee(LATEST_ANSWER)
            }
        }

    @Test
    fun `GIVEN a conversation that fits the screen WHEN it is shown THEN the question is still shown`() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            setContent(conversation(stateNamed("A finished exchange")))
            waitForIdle()

            onConversationScroll { seesItStillShown(QUESTION) }
        }

    /**
     * A chat turned off after a long secret part, with a few turns since: at its newest message the
     * part's band sits on screen above them. [open] is the part read with the unlock.
     */
    private fun turnedOff(open: Boolean): ChatSessionUiState {
        fun message(
            id: String,
            content: String,
            secret: Boolean = false,
        ) = ChatMessage(
            id = id,
            sessionId = "s-1",
            role = if (id.last().digitToInt() % 2 == 0) MessageRole.ASSISTANT else MessageRole.USER,
            content = if (secret && !open) "" else content,
            isSecret = secret,
            withheld = secret && !open,
        )
        val before = (1..3).map { message("before-$it", "Turn $it before the secret part, a line or two long.") }
        val part =
            (1..8).map {
                message(
                    "secret-$it",
                    if (it ==
                        1
                    ) {
                        FIRST_SECRET_WORDS
                    } else {
                        "Secret turn $it, long enough to fill two lines of the screen."
                    },
                    secret = true,
                )
            }
        val after = listOf(message("after-1", "Add the plumber on Monday"), message("after-2", LATEST_ANSWER))
        return ChatSessionUiState(
            session = ConversationSession(id = "s-1", userId = "emma", hasSecretPart = true),
            messages = before + part + after,
            agentName = "Home Coordinator",
            secretParts = listOf(SecretPart(from = 3, until = 11)),
        )
    }

    @Test
    fun `GIVEN a chat at its newest message with a locked secret part on screen WHEN the part opens THEN it opens where the band was and nothing jumps to the end`() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            var state by mutableStateOf(turnedOff(open = false))
            setContent(growing { state })
            onConversationScroll { waitsForText(LATEST_ANSWER) }

            state = turnedOff(open = true)
            waitForIdle()

            onConversationScroll {
                seesItStillShown(FIRST_SECRET_WORDS)
                doesNotSee(LATEST_ANSWER)
            }
        }

    @Test
    fun `GIVEN a chat at its newest message with an open secret part above WHEN the part folds back THEN the newest message is still shown`() =
        runComposeUiTest(effectContext = StandardTestDispatcher()) {
            var state by mutableStateOf(turnedOff(open = true))
            setContent(growing { state })
            onConversationScroll { waitsForText(LATEST_ANSWER) }

            state = turnedOff(open = false)
            waitForIdle()

            onConversationScroll { seesItStillShown(LATEST_ANSWER) }
        }

    private companion object {
        const val FIRST_SECRET_WORDS = "Plan the surprise for Saturday"
        val PHONE_WIDTH = 390.dp
        val PHONE_HEIGHT = 844.dp
        const val QUESTION = "What's left before Friday?"
        const val NEWEST_ANSWER = "Answer 20, at some length to fill the screen."
        const val OLDEST_QUESTION = "Question 1 of a long-running conversation"
        const val LATEST_ANSWER = "One more answer, after the fold."
    }
}
