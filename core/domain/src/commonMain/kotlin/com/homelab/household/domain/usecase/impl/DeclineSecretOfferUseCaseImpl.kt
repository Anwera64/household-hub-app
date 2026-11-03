package com.homelab.household.domain.usecase.impl

import com.homelab.household.domain.repository.SessionRepository
import com.homelab.household.domain.usecase.DeclineSecretOfferUseCase

class DeclineSecretOfferUseCaseImpl(
    private val sessionRepository: SessionRepository,
) : DeclineSecretOfferUseCase {
    override suspend operator fun invoke(sessionId: String) = sessionRepository.declineSecretOffer(sessionId)
}
