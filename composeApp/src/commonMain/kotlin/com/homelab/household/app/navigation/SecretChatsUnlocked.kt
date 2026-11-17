package com.homelab.household.app.navigation

/**
 * What the secret chats PIN pad sends back when the PIN opens them, over Navigation 3's
 * `ResultEventBus`. The chat that opened it reads itself again, its secret parts open in place;
 * [thenSend] carries the pad's own, so a send the hub refused as locked goes once it reads.
 */
data class SecretChatsUnlocked(
    val thenSend: Boolean,
)
