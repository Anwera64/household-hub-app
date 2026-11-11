package com.homelab.household.domain.usecase.impl

import com.homelab.household.domain.repository.HubEventsRepository
import com.homelab.household.domain.usecase.ObserveHubUnreachableUseCase
import kotlinx.coroutines.flow.Flow

class ObserveHubUnreachableUseCaseImpl(
    private val hubEvents: HubEventsRepository,
) : ObserveHubUnreachableUseCase {
    override operator fun invoke(): Flow<Unit> = hubEvents.observeUnreachable()
}
