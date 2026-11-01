package com.homelab.household.app.screens.secretunlock

import androidx.compose.ui.test.ExperimentalTestApi
import com.homelab.household.app.testing.FakeSecretChatHub
import com.homelab.household.app.testing.runScreenTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A secret chat opened with nothing unlocked, through the app's real wiring down to the HTTP
 * engine: the hub answers 423, and the screen asks for the PIN instead of starting a new chat.
 */
@OptIn(ExperimentalTestApi::class)
class SecretChatTest {
    @Test
    fun `GIVEN a secret chat with nothing unlocked WHEN it is opened THEN the PIN pad shows and no new chat`() =
        runScreenTest {
            secretChatScreen(FakeSecretChatHub())

            onSecretChat {
                seesItLocked()
                seesNoGreetingAndNoComposer()
            }
        }

    @Test
    fun `GIVEN a locked secret chat WHEN a wrong PIN is typed THEN it says so and stays locked`() =
        runScreenTest {
            val hub = FakeSecretChatHub()
            secretChatScreen(hub)

            onSecretChat {
                seesItLocked()
                typesThePin("000000")
                seesTheWrongPin()
                seesItLocked()
            }
            assertEquals(1, hub.unlockAttempts)
        }

    @Test
    fun `GIVEN a locked secret chat WHEN the right PIN is typed THEN the chat opens in place`() =
        runScreenTest {
            secretChatScreen(FakeSecretChatHub())

            onSecretChat {
                seesItLocked()
                typesThePin(FakeSecretChatHub.RIGHT_PIN)
                seesTheSecretWords()
            }
        }
}
