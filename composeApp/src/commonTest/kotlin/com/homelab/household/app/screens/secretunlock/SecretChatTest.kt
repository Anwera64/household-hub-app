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
 * engine. All secret, it shows the locked screen instead of starting a new chat; part secret, its
 * secret parts fold into bands. The PIN pad is a destination of its own, opened over the chat.
 */
@OptIn(ExperimentalTestApi::class)
class SecretChatTest {
    @Test
    fun `GIVEN a chat started secret on a hub that reads it whole WHEN it is opened with nothing unlocked THEN the locked screen shows and no new chat`() =
        runScreenTest {
            secretChatScreen(FakeSecretChatHub())

            onSecretChat {
                seesItLocked()
                seesNoGreetingAndNoComposer()
            }
        }

    @Test
    fun `GIVEN a chat started secret on a hub that reads by section WHEN it is opened with nothing unlocked THEN the locked screen shows and no new chat`() =
        runScreenTest {
            secretChatScreen(FakeSecretChatHub(), sessionId = "s-3")

            onSecretChat {
                seesItLocked()
                seesNoGreetingAndNoComposer()
                seesNoSecretWords()
            }
        }

    @Test
    fun `GIVEN a locked chat WHEN Unlock is tapped THEN the secret chats PIN pad is asked for once`() =
        runScreenTest {
            val navigation = ChatNavigation()
            secretChatScreen(FakeSecretChatHub(), sessionId = "s-3", navigation = navigation)

            onSecretChat(navigation) {
                seesItLocked()
                unlocks()
            }
            assertEquals(1, navigation.pinPadsAsked)
        }

    @Test
    fun `GIVEN a locked chat WHEN the PIN opens secret chats on the pad THEN the chat opens in place`() =
        runScreenTest(effectContext = StandardTestDispatcher()) {
            val navigation = ChatNavigation()
            secretChatScreen(FakeSecretChatHub(), sessionId = "s-3", navigation = navigation)

            onSecretChat(navigation) {
                seesItLocked()
                opensSecretChatsOnThePad()
                seesTheSecretWords()
                seesTheComposer()
            }
        }

    @Test
    fun `GIVEN a chat with ordinary messages and then Secret Mode on WHEN it is opened with nothing unlocked THEN its ordinary messages show with one locked band and the message box`() =
        runScreenTest {
            secretChatScreen(FakeSecretChatHub(), sessionId = "s-4")

            onSecretChat {
                seesTheOrdinaryMessagesBeforeSecretMode()
                seesTheLockedPart()
                seesNoSecretWords()
                seesTheComposer()
            }
        }

    @Test
    fun `GIVEN a chat with Secret Mode on and nothing unlocked WHEN a message is sent THEN the words stay in the box and the PIN pad is asked for before the hub`() =
        runScreenTest(effectContext = StandardTestDispatcher()) {
            val hub = FakeSecretChatHub()
            val navigation = ChatNavigation()
            secretChatScreen(hub, sessionId = "s-4", navigation = navigation)

            onSecretChat(navigation) {
                seesTheOrdinaryMessagesBeforeSecretMode()
                says("And the flowers?")
                seesInTheBox("And the flowers?")
            }
            assertEquals(1, navigation.pinPadsAskedToSend)
            assertEquals(0, hub.secretSends)
        }

    @Test
    fun `GIVEN a chat with Secret Mode on and nothing unlocked and words typed in the box WHEN the keyboard's Send is tapped THEN the box lets go of the keyboard before the PIN pad opens`() =
        runScreenTest(effectContext = StandardTestDispatcher()) {
            val hub = FakeSecretChatHub()
            val navigation = ChatNavigation()
            secretChatScreen(hub, sessionId = "s-4", navigation = navigation)

            onSecretChat(navigation) {
                seesTheOrdinaryMessagesBeforeSecretMode()
                types("And the flowers?")
                seesTheBoxFocused()
                sendsFromTheKeyboard()
                seesInTheBox("And the flowers?")
                seesTheBoxLetGoOfTheKeyboard()
            }
            assertEquals(1, navigation.pinPadsAskedToSend)
        }

    @Test
    fun `GIVEN an open secret chat WHEN Secret Mode is turned off on the sheet THEN the hub turns it off and the chat says so`() =
        runScreenTest(effectContext = StandardTestDispatcher()) {
            val hub = FakeSecretChatHub()
            val navigation = ChatNavigation()
            secretChatScreen(hub, navigation = navigation)

            onSecretChat(navigation) {
                opensSecretChatsOnThePad()
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
    fun `GIVEN a locked secret part WHEN Unlock is tapped THEN the secret chats PIN pad is asked for over the chat`() =
        runScreenTest {
            val navigation = ChatNavigation()
            secretChatScreen(FakeSecretChatHub(), sessionId = "s-2", navigation = navigation)

            onSecretChat(navigation) {
                seesTheLockedPart()
                unlocks()
                seesTheOrdinaryPartsOfTheChat()
            }
            assertEquals(1, navigation.pinPadsAsked)
        }

    @Test
    fun `GIVEN a locked secret part WHEN the PIN opens secret chats on the pad THEN the part opens in place`() =
        runScreenTest(effectContext = StandardTestDispatcher()) {
            val navigation = ChatNavigation()
            secretChatScreen(FakeSecretChatHub(), sessionId = "s-2", navigation = navigation)

            onSecretChat(navigation) {
                seesTheLockedPart()
                opensSecretChatsOnThePad()
                seesThePartOpenInPlace()
            }
        }

    @Test
    fun `GIVEN a chat unlocked with the right PIN that has locked again WHEN its Unlock is tapped and two digits typed THEN the pad takes them`() =
        runScreenTest(effectContext = StandardTestDispatcher()) {
            secretChatInTheApp(FakeSecretChatHub())

            onSecretChat {
                seesItLocked()
                unlocks()
                typesThePin(FakeSecretChatHub.RIGHT_PIN)
                seesTheSecretWords()
                locksSecretChats()
                seesItLocked()

                unlocks()
                typesThePin("48")

                seesDigitsEntered(2)
                seesThePinNotBeingChecked()
            }
        }
}
