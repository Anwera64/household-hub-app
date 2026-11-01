package com.homelab.household.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SecretUnlockRequestDto(
    val pin: String,
)

/** Opens every secret chat on this phone for `expires_in` seconds (locking spec §4.1). */
@Serializable
data class SecretUnlockReadDto(
    @SerialName("unlock_token") val unlock_token: String,
    @SerialName("expires_in") val expires_in: Int,
)
