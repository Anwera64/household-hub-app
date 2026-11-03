package com.homelab.household.app.navigation

/**
 * What the secret chats PIN pad sends back when the PIN opens them, over Navigation 3's
 * `ResultEventBus`. A chat that opened it from its locked secret part reads itself again, and the
 * part opens in place.
 */
data object SecretChatsUnlocked
