package com.homelab.household.data.repository

import com.homelab.household.data.datasource.local.HubEventsLocalDataSource
import com.homelab.household.domain.repository.HubEventsRepository
import kotlinx.coroutines.flow.Flow

class HubEventsRepositoryImpl(
    private val local: HubEventsLocalDataSource,
) : HubEventsRepository {
    override fun observeUnreachable(): Flow<Unit> = local.observeUnreachable()
}
