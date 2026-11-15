package com.homelab.household.data.repository

import com.homelab.household.data.datasource.local.LocalNetworkPreferencesLocalDataSource
import com.homelab.household.domain.repository.LocalNetworkRepository

class LocalNetworkRepositoryImpl(
    private val local: LocalNetworkPreferencesLocalDataSource,
) : LocalNetworkRepository {
    override fun explained(): Boolean = local.explained()

    override fun markExplained() = local.markExplained()

    override fun stoppedAsking(): Boolean = local.stoppedAsking()

    override fun stopAsking() = local.stopAsking()
}
