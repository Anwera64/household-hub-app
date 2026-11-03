package com.homelab.household.domain.usecase

/** "Don't ask again" on the sheet that turns Secret Mode off, for the member signed in on this phone. */
fun interface StopAskingBeforeSecretOffUseCase {
    suspend operator fun invoke()
}
