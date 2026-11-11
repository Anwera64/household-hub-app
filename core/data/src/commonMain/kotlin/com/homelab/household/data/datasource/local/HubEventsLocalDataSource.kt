package com.homelab.household.data.datasource.local

import co.touchlab.kermit.Logger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * What happens to calls to the hub, as events: for now, that one found no route. Raised by
 * `reportUnreachable` on the app's HTTP client, the way `AuthEventsLocalDataSource` is raised by
 * `signOutOnUnauthorized`.
 */
class HubEventsLocalDataSource(
    private val log: Logger = Logger.withTag("Hub"),
) {
    /** Nothing is replayed: a listener starting later must not act on an old failure. */
    private val unreachable =
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    fun observeUnreachable(): Flow<Unit> = unreachable.asSharedFlow()

    fun raiseUnreachable() {
        log.i { "A hub call found no route" }
        unreachable.tryEmit(Unit)
    }
}
