package com.homelab.household.localnetwork

import com.homelab.household.domain.localnetwork.LocalNetworkAccess
import com.homelab.household.domain.localnetwork.LocalNetworkStatus

/** A desktop JVM has no local network permission. */
class JvmLocalNetworkAccess : LocalNetworkAccess {
    override val readsWithoutAsking: Boolean = true

    override suspend fun status(): LocalNetworkStatus = LocalNetworkStatus.NotNeeded
}
