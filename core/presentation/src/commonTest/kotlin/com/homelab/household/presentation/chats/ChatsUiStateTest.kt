package com.homelab.household.presentation.chats

import com.homelab.household.domain.model.ConversationSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The bar at the top of Chats (canvas Chats and ChatsUnlocked): it unlocks every secret chat at
 * once, and once they are open it offers to lock them again. It only appears when there are
 * secret chats, and never over a search, which leaves them out anyway.
 */
class ChatsUiStateTest {
    private val ordinary = ConversationSession(id = "s-1", userId = "u-1", title = "Saturday dinner")
    private val lockedChat =
        ConversationSession(
            id = "s-2",
            userId = "u-1",
            title = "",
            isSecret = true,
            hasSecretPart = true,
            isLocked = true,
        )
    private val openChat = lockedChat.copy(id = "s-3", title = "Anniversary surprise", isLocked = false)
    private val endedSecretPart = ordinary.copy(id = "s-4", title = "Lisbon offer", hasSecretPart = true)

    private fun chats(vararg sessions: ConversationSession) =
        ChatsUiState(loaded = true, total = sessions.size, visible = sessions.toList())

    @Test
    fun `GIVEN only ordinary chats WHEN the list shows THEN there is no bar`() {
        assertNull(chats(ordinary).secretBar)
    }

    @Test
    fun `GIVEN two locked secret chats WHEN the list shows THEN the bar counts both as locked`() {
        val state = chats(ordinary, lockedChat, lockedChat.copy(id = "s-5"))

        assertEquals(SecretBar.Locked(count = 2), state.secretBar)
    }

    @Test
    fun `GIVEN secret chats that are all open WHEN the list shows THEN the bar offers to lock them`() {
        assertEquals(SecretBar.Unlocked, chats(ordinary, openChat, endedSecretPart).secretBar)
    }

    @Test
    fun `GIVEN one open and one locked secret chat WHEN the list shows THEN the bar still asks to unlock`() {
        assertEquals(SecretBar.Locked(count = 1), chats(openChat, lockedChat).secretBar)
    }

    @Test
    fun `GIVEN locked secret chats WHEN a search is typed THEN the bar is gone`() {
        assertNull(chats(ordinary, lockedChat).copy(query = "dinner").secretBar)
    }
}
