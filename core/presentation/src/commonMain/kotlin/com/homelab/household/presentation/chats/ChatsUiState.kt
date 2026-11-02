package com.homelab.household.presentation.chats

import com.homelab.household.domain.model.ConversationSession

data class ChatsUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val visible: List<ConversationSession> = emptyList(),
    val errorMessage: String? = null,
    /**
     * Whether a load has actually succeeded, and how many conversations it found.
     *
     * Both exist only to tell three things apart that all show no rows: still loading, a household
     * with no chats, and a search that matched none. Screens read [isEmpty] and [hasNoMatches]
     * rather than these.
     */
    val loaded: Boolean = false,
    val total: Int = 0,
) {
    /**
     * A household that has never chatted, which is a screen of its own — no search, no secret bar,
     * one button. Both of those arrive with the first chat.
     *
     * Deliberately false until a load has actually succeeded: an unreachable hub returns no rows
     * either, and offering "Start a chat" to someone whose hub is down would be answering the
     * wrong question.
     */
    val isEmpty: Boolean
        get() = loaded && total == 0

    /** A query that found nothing, which reads differently: the chats exist, this one doesn't. */
    val hasNoMatches: Boolean
        get() = loaded && total > 0 && query.isNotBlank() && visible.isEmpty()

    /**
     * The bar over the list: none without secret chats, and none over a search, which leaves them
     * out. One locked chat is enough to ask for the PIN, since the PIN opens them all.
     */
    val secretBar: SecretBar?
        get() {
            if (query.isNotBlank()) return null
            val locked = visible.count { it.isLocked }
            return when {
                locked > 0 -> SecretBar.Locked(locked)
                visible.any { it.isSecret || it.hasSecretPart } -> SecretBar.Unlocked
                else -> null
            }
        }
}

/** What the bar over the Chats list offers. */
sealed interface SecretBar {
    /** [count] secret chats are locked; Unlock asks for the PIN. */
    data class Locked(
        val count: Int,
    ) : SecretBar

    /** Every secret chat is open; Lock now closes them again. */
    data object Unlocked : SecretBar
}
