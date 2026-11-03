package com.homelab.household.app.screens.chats

import androidx.compose.ui.test.ExperimentalTestApi
import com.homelab.household.app.screens.secretunlock.onSecretChat
import com.homelab.household.app.testing.FakeSecretChatHub
import com.homelab.household.app.testing.runScreenTest
import kotlin.test.Test

/**
 * Secret chats on the Chats list, through the app's real wiring down to the HTTP engine: the hub
 * holds their titles back until the request carries an unlock, and the bar over the list is the way
 * to send one, or to throw it away.
 */
@OptIn(ExperimentalTestApi::class)
class SecretChatsListTest {
    @Test
    fun `GIVEN a locked secret chat on the list WHEN it is unlocked from the bar THEN the list shows its title`() =
        runScreenTest {
            secretChatsListScreen(FakeSecretChatHub())

            onChats {
                seesSecretChatsLocked(count = 1)
                seesChat("Private conversation")
                // Its last message is ordinary, so the hub sends it while locked.
                seesChat(FakeSecretChatHub.SECRET_PREVIEW)
                tapsUnlock()
            }
            onSecretChat {
                seesUnlockSecretChats()
                typesThePin(FakeSecretChatHub.RIGHT_PIN)
            }
            onChats {
                seesChat(FakeSecretChatHub.SECRET_TITLE)
                seesSecretChatsUnlocked()
            }
        }

    @Test
    fun `GIVEN secret chats unlocked from the bar WHEN Lock now is tapped THEN their titles are held back again`() =
        runScreenTest {
            secretChatsListScreen(FakeSecretChatHub())
            onChats { tapsUnlock() }
            onSecretChat { typesThePin(FakeSecretChatHub.RIGHT_PIN) }

            onChats {
                seesChat(FakeSecretChatHub.SECRET_TITLE)
                tapsLockNow()
                seesChat("Private conversation")
                seesSecretChatsLocked(count = 1)
            }
        }

    @Test
    fun `GIVEN a hub that does not answer WHEN the PIN is typed on the bar's pad THEN the pad says nothing answered`() =
        runScreenTest {
            val hub = FakeSecretChatHub().apply { answering = false }
            secretChatsListScreen(hub)
            onChats { tapsUnlock() }

            onSecretChat {
                typesThePin(FakeSecretChatHub.RIGHT_PIN)
                seesTheHubDidNotAnswer()
                seesUnlockSecretChats()
            }
        }
}
