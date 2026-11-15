package com.homelab.household.domain.localnetwork

/** What the cover says once it has gone, in a snackbar. */
enum class LocalNetworkEvent {
    /** Access was turned on while the cover was up. */
    BackOnWifi,

    /** The member chose "Don't ask again on this phone". */
    StoppedAsking,
}
