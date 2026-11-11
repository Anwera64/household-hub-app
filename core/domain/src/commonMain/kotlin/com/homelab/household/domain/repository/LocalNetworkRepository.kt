package com.homelab.household.domain.repository

/** What this phone remembers about local network access, across restarts. */
interface LocalNetworkRepository {
    /** Whether the app has explained local network access on this phone. */
    fun explained(): Boolean

    fun markExplained()

    /** Whether the member chose "Don't ask again on this phone". */
    fun stoppedAsking(): Boolean

    fun stopAsking()
}
