package com.homelab.household.domain.usecase

/**
 * "Not now" on an offer of Secret Mode. The agent stops offering in that chat; a message the hub
 * judges private still offers.
 */
fun interface DeclineSecretOfferUseCase {
    suspend operator fun invoke(sessionId: String)
}
