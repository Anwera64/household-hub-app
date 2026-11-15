package com.homelab.household.domain.usecase.impl

import com.homelab.household.domain.localnetwork.LocalNetworkAccess
import com.homelab.household.domain.localnetwork.LocalNetworkStatus
import com.homelab.household.domain.usecase.GetLocalNetworkStatusUseCase

class GetLocalNetworkStatusUseCaseImpl(
    private val access: LocalNetworkAccess,
) : GetLocalNetworkStatusUseCase {
    override suspend operator fun invoke(): LocalNetworkStatus = access.status()
}
