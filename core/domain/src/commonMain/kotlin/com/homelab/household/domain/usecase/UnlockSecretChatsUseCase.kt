package com.homelab.household.domain.usecase

/** Trades the member's PIN for ten minutes of every secret chat on this phone. Throws as sign-in does. */
fun interface UnlockSecretChatsUseCase {
    suspend operator fun invoke(pin: String)
}
