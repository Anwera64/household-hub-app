package com.homelab.household.app.screens.chats

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.homelab.household.domain.model.ConversationSession
import com.homelab.household.presentation.chats.ChatAge
import com.homelab.household.presentation.chats.ChatsUiState

/** The Chats list in each state worth drawing. `ChatsScreenTest` renders them all. */
class ChatsUiStateProvider : PreviewParameterProvider<ChatsUiState> {
    private fun chat(
        id: String,
        title: String,
        preview: String,
        avatar: String = "🏡",
    ) = ConversationSession(
        id = id,
        userId = "u-1",
        title = title,
        agentName = if (avatar == "🏡") "Home & Life Coordinator" else "Academic & Document Researcher",
        agentAvatar = avatar,
        lastMessagePreview = preview,
    )

    private val dinner =
        chat("s-1", "Saturday dinner", "Added. I've let the household know you're both out Saturday evening.")
    private val papers =
        chat(
            "s-2",
            "Jury panel references",
            "I pulled three papers on adaptive reuse — the Lacaton & Vassal one is closest.",
            avatar = "📚",
        )
    private val groceries =
        chat(
            "s-3",
            "Groceries for the week",
            "Liam's picking up the veg box Thursday, so skip the market run.",
        )

    private val all = listOf(dinner, papers, groceries)

    // Two secret chats as the hub sends them with nothing unlocked: no title, no preview.
    private val anniversary =
        ConversationSession(
            id = "s-4",
            userId = "u-1",
            title = "",
            isSecret = true,
            hasSecretPart = true,
            isLocked = true,
            agentAvatar = "🏡",
        )
    private val lisbon = anniversary.copy(id = "s-5", agentAvatar = "📚")
    private val withSecrets = listOf(dinner, anniversary, papers, groceries, lisbon)

    // The same two once the PIN has opened them (canvas ChatsUnlocked).
    private val unlocked =
        listOf(
            dinner,
            anniversary.copy(
                title = "Anniversary surprise",
                lastMessagePreview = "The cabin is free on the 14th — I've kept it off the shared calendar.",
                isLocked = false,
            ),
            papers,
            groceries,
            lisbon.copy(
                title = "Thinking about the Lisbon offer",
                lastMessagePreview = "The relocation package covers six months of rent, which changes the maths.",
                isLocked = false,
            ),
        )

    private val named =
        listOf(
            "The list" to ChatsUiState(visible = all, loaded = true, total = all.size),
            "Searching" to
                ChatsUiState(query = "dinner", visible = listOf(dinner), loaded = true, total = all.size),
            "Nothing matched" to
                ChatsUiState(query = "boiler", visible = emptyList(), loaded = true, total = all.size),
            // The one empty screen with a button: it is the only one you are expected to act on.
            "Never chatted" to ChatsUiState(visible = emptyList(), loaded = true, total = 0),
            "Arriving" to ChatsUiState(isLoading = true),
            "Coming back" to ChatsUiState(isLoading = true, visible = all, loaded = true, total = all.size),
            "Hub unreachable" to ChatsUiState(errorMessage = "Can't reach your hub"),
            // Secret, and nothing unlocked: the hub sends no title and no preview.
            "A locked secret chat" to
                ChatsUiState(
                    visible =
                        listOf(
                            ConversationSession(
                                id = "s-7",
                                userId = "u-1",
                                title = "",
                                isSecret = true,
                                hasSecretPart = true,
                                isLocked = true,
                                agentAvatar = "🏡",
                            ),
                        ),
                    loaded = true,
                    total = 1,
                ),
            "Secret chats locked" to ChatsUiState(visible = withSecrets, loaded = true, total = withSecrets.size),
            "Secret chats unlocked" to ChatsUiState(visible = unlocked, loaded = true, total = unlocked.size),
            // A conversation that outlived its agent: no emoji to draw, and it must not collapse.
            "An agent that is gone" to
                ChatsUiState(
                    visible =
                        listOf(
                            ConversationSession(
                                id = "s-9",
                                userId = "u-1",
                                title = "Is the 5080 worth it?",
                                lastMessagePreview = "That settles it, thanks",
                            ),
                        ),
                    loaded = true,
                    total = 1,
                ),
        )

    // When each chat last moved, as the canvas rows read: "10m", "Tue", "12 Oct", "Yesterday", "Sun".
    private val ages =
        mapOf(
            "s-1" to ChatAge.Minutes(10),
            "s-2" to ChatAge.Weekday(isoDay = 2),
            "s-3" to ChatAge.Date(day = 12, month = 10, year = null),
            "s-4" to ChatAge.Yesterday,
            "s-5" to ChatAge.Weekday(isoDay = 7),
            "s-7" to ChatAge.Yesterday,
            "s-9" to ChatAge.Date(day = 26, month = 10, year = 2025),
        )

    override val values: Sequence<ChatsUiState> = named.map { it.second.copy(ages = ages) }.asSequence()

    override fun getDisplayName(index: Int): String = named[index].first
}
