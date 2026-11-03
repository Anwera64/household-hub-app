package com.homelab.household.data.repository

import com.homelab.household.data.datasource.local.SecretPreferencesLocalDataSource
import com.homelab.household.domain.repository.SecretPreferencesRepository

class SecretPreferencesRepositoryImpl(
    private val local: SecretPreferencesLocalDataSource,
) : SecretPreferencesRepository {
    override fun asksBeforeTurningOff(memberId: String): Boolean = local.asksBeforeTurningOff(memberId)

    override fun stopAskingBeforeTurningOff(memberId: String) = local.stopAskingBeforeTurningOff(memberId)
}
