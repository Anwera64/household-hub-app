package com.homelab.household.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** The two buttons on a fact not saved yet: Don't save · Save. */
enum class HeldFactAnswer {
    Save,
    DontSave,
}

/**
 * A fact an agent read in a chat holding a private message, not saved yet: the hub couldn't check it
 * against the chat's private messages, so it waits for the member to Save it or not. Only the chat's
 * owner ever sees one, so [sourceTitle] is set. One with [expiresAt] goes when that day passes.
 */
@OptIn(ExperimentalTime::class)
data class HeldFact(
    val id: String,
    val content: String,
    val scope: MemoryScope,
    val agentName: String,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val sourceTitle: String?,
    val sourceOwnerName: String,
    val readAt: Instant,
)
