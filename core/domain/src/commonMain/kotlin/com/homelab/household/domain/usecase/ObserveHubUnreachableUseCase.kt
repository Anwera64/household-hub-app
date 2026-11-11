package com.homelab.household.domain.usecase

import kotlinx.coroutines.flow.Flow

/** A call to the hub found no route. Events, never replayed. */
fun interface ObserveHubUnreachableUseCase {
    operator fun invoke(): Flow<Unit>
}
