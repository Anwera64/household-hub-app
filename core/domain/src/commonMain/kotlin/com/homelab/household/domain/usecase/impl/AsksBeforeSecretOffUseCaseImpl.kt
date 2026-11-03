package com.homelab.household.domain.usecase.impl

import com.homelab.household.domain.repository.AuthRepository
import com.homelab.household.domain.repository.SecretPreferencesRepository
import com.homelab.household.domain.usecase.AsksBeforeSecretOffUseCase

class AsksBeforeSecretOffUseCaseImpl(
    private val authRepository: AuthRepository,
    private val preferences: SecretPreferencesRepository,
) : AsksBeforeSecretOffUseCase {
    /** A phone that cannot tell who is signed in asks: the sheet is the safe side. */
    override suspend operator fun invoke(): Boolean {
        val member = authRepository.getCurrentUser() ?: return true
        return preferences.asksBeforeTurningOff(member.id)
    }
}
