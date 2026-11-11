package com.homelab.household.domain.usecase

import com.homelab.household.domain.localnetwork.LocalNetworkStatus

/** Whether this phone lets the app reach the hub on the home network. On iOS it takes up to 3 s. */
fun interface GetLocalNetworkStatusUseCase {
    suspend operator fun invoke(): LocalNetworkStatus
}
