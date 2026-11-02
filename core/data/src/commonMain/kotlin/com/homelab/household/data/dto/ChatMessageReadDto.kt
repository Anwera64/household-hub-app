package com.homelab.household.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ChatMessageReadDto(
    val id: String,
    @SerialName("session_id") val session_id: String,
    val role: String,
    val content: String,
    @SerialName("created_at") val created_at: String? = null,
    @SerialName("metadata_json") val metadata_json: JsonObject? = null,
    /** Said while the chat was secret. */
    @SerialName("is_secret") val is_secret: Boolean = false,
    /** The hub kept this message back: it comes with empty content and no metadata. */
    val withheld: Boolean = false,
)
