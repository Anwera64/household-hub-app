package com.homelab.household.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatTurnRequestDto(
    val content: String,
)
