package com.homelab.household.domain.usecase.impl

import com.homelab.household.domain.repository.SecretUnlockRepository
import com.homelab.household.domain.usecase.UnlockSecretChatsUseCase

class UnlockSecretChatsUseCaseImpl(
    private val unlocks: SecretUnlockRepository,
) : UnlockSecretChatsUseCase {
    override suspend operator fun invoke(pin: String) = unlocks.unlock(pin)
}
