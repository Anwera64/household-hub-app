package com.homelab.household.app.screens.conversation

import com.homelab.household.domain.model.ChatMessage
import com.homelab.household.presentation.chatsession.SecretPart

/** What the transcript draws, in order: the messages, and the lines and bands that sit between them. */
internal sealed interface TranscriptRow {
    val key: Any

    /** Fades in and out as it comes and goes: what a secret part opening or folding back changes. */
    val fades: Boolean get() = true

    /** [fades] for a message of a secret part that has ended, which opens and folds in place. */
    data class Message(
        val message: ChatMessage,
        override val fades: Boolean = false,
    ) : TranscriptRow {
        override val key: Any get() = message.id
    }

    /** The quiet "Looked over" line, after the last message it read. */
    data object LookedOver : TranscriptRow {
        override val key: Any get() = "looked-over"
        override val fades: Boolean get() = false
    }

    /** Where a secret part starts. [live] is the part the chat is in now, which keeps its promise. */
    data class SecretStart(
        val at: Int,
        val live: Boolean,
    ) : TranscriptRow {
        override val key: Any get() = "secret-start-$at"
    }

    /** Where the chat stopped being secret. */
    data class SecretEnd(
        val at: Int,
    ) : TranscriptRow {
        override val key: Any get() = "secret-end-$at"
    }

    /** A secret part the hub withheld, as one band in place of its [messages]. */
    data class LockedPart(
        val at: Int,
        val messages: Int,
    ) : TranscriptRow {
        override val key: Any get() = "secret-part-$at"
    }
}

/**
 * Lays [messages] out with the lines between them: the look-over line after [lookedOverCut]
 * messages (null when it is not drawn), and each of [secretParts] marked where it starts and ends.
 * A part whose every message the hub withheld folds into one band instead of a start marker.
 *
 * Where several fall between the same two messages, the look-over line comes first, since it
 * belongs to the message above it; then the end of one secret part, then the start of the next.
 */
internal fun transcriptRows(
    messages: List<ChatMessage>,
    secretParts: List<SecretPart>,
    lookedOverCut: Int?,
): List<TranscriptRow> {
    val rows = mutableListOf<TranscriptRow>()
    val starts = secretParts.associateBy { it.from }
    val ends = secretParts.mapNotNull { it.until }.toSet()
    var at = 0
    while (at <= messages.size) {
        if (at == lookedOverCut) rows += TranscriptRow.LookedOver
        if (at in ends) rows += TranscriptRow.SecretEnd(at)
        val part = starts[at]
        if (part != null) {
            val until = (part.until ?: messages.size).coerceAtMost(messages.size)
            val held = messages.subList(at, until)
            if (held.isNotEmpty() && held.all { it.withheld }) {
                rows += TranscriptRow.LockedPart(at, held.size)
                at = until
                continue
            }
            rows += TranscriptRow.SecretStart(at, live = part.until == null)
        }
        if (at < messages.size) rows += TranscriptRow.Message(messages[at], fades = secretParts.endedAround(at))
        at++
    }
    return rows
}

/** Whether the message at [at] is in a secret part that has ended. */
private fun List<SecretPart>.endedAround(at: Int): Boolean =
    any { part ->
        part.until.let {
            it != null &&
                at in part.from until it
        }
    }
