package com.homelab.household.domain.usecase.impl

import com.homelab.household.domain.repository.AuthRepository
import com.homelab.household.domain.repository.SecretUnlockRepository
import com.homelab.household.domain.usecase.LogoutUseCase

class LogoutUseCaseImpl(
    private val authRepository: AuthRepository,
    private val secretUnlocks: SecretUnlockRepository,
) : LogoutUseCase {
    override suspend operator fun invoke() {
        secretUnlocks.lock()
        authRepository.logout()
    }
}
