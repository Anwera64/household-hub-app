package com.homelab.household.app.screens.secretunlock

import androidx.compose.ui.test.ExperimentalTestApi
import com.homelab.household.app.testing.FakeSecretChatHub
import com.homelab.household.app.testing.runScreenTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun `GIVEN an open secret chat WHEN Secret Mode is turned off on the sheet THEN the hub turns it off and the chat says so`() =
        runScreenTest(effectContext = StandardTestDispatcher()) {
            val hub = FakeSecretChatHub()
            secretChatScreen(hub)

            onSecretChat {
                typesThePin(FakeSecretChatHub.RIGHT_PIN)
                seesTheSecretWords()
                turnsSecretModeOff()
                seesItNoLongerSecret()
                seesTheSecretWords()
            }
            assertEquals(1, hub.secretToggles.size)
            assertTrue("\"is_secret\":false" in hub.secretToggles.single())
        }

    @Test
    fun `GIVEN a chat turned off with nothing unlocked WHEN it is opened THEN it reads with its secret part as one locked band`() =
        runScreenTest {
            secretChatScreen(FakeSecretChatHub(), sessionId = "s-2")

            onSecretChat {
                seesTheOrdinaryPartsOfTheChat()
                seesTheLockedPart()
                seesNoSecretWords()
            }
        }

    @Test
    fun `GIVEN a locked secret part WHEN Unlock is tapped and the right PIN typed THEN the part opens in place`() =
        runScreenTest(effectContext = StandardTestDispatcher()) {
            secretChatScreen(FakeSecretChatHub(), sessionId = "s-2")

            onSecretChat {
                seesTheLockedPart()
                unlocksThePart()
                seesItLocked()
                typesThePin(FakeSecretChatHub.RIGHT_PIN)
                seesThePartOpenInPlace()
            }
        }

    @Test
    fun `GIVEN the PIN pad for a secret part WHEN the phone's back gesture is used THEN the chat shows again with the part locked`() =
        runScreenTest {
            val systemBack = SystemBack()
            secretChatScreen(FakeSecretChatHub(), sessionId = "s-2", systemBack = systemBack)

            onSecretChat(systemBack) {
                seesTheLockedPart()
                unlocksThePart()
                seesItLocked()
                swipesBack()
                seesTheOrdinaryPartsOfTheChat()
                seesTheLockedPart()
            }
            assertEquals(0, systemBack.leftTheChat)
        }

    @Test
    fun `GIVEN the PIN pad for a secret part WHEN Back is tapped THEN the chat shows again with the part locked`() =
        runScreenTest {
            secretChatScreen(FakeSecretChatHub(), sessionId = "s-2")

            onSecretChat {
                seesTheLockedPart()
                unlocksThePart()
                seesItLocked()
                goesBack()
                seesTheOrdinaryPartsOfTheChat()
                seesTheLockedPart()
            }
        }
}
