package com.homelab.household.domain.repository

import kotlinx.coroutines.flow.Flow

/** What happens to calls to the hub, as events rather than state. */
interface HubEventsRepository {
    /** A call to the hub found no route: a timeout, a refused connection, no network. Never replayed. */
    fun observeUnreachable(): Flow<Unit>
}
