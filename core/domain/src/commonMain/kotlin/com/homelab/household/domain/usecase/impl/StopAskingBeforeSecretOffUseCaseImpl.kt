package com.homelab.household.domain.usecase.impl

import com.homelab.household.domain.repository.AuthRepository
import com.homelab.household.domain.repository.SecretPreferencesRepository
import com.homelab.household.domain.usecase.StopAskingBeforeSecretOffUseCase

class StopAskingBeforeSecretOffUseCaseImpl(
    private val authRepository: AuthRepository,
    private val preferences: SecretPreferencesRepository,
) : StopAskingBeforeSecretOffUseCase {
    override suspend operator fun invoke() {
        val member = authRepository.getCurrentUser() ?: return
        preferences.stopAskingBeforeTurningOff(member.id)
    }
}
