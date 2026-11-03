package com.homelab.household.domain.usecase

/** Whether the member signed in on this phone is asked before Secret Mode turns off. */
fun interface AsksBeforeSecretOffUseCase {
    suspend operator fun invoke(): Boolean
}
