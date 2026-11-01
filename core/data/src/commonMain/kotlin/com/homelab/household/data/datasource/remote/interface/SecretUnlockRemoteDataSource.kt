package com.homelab.household.data.datasource.remote.`interface`

import com.homelab.household.data.dto.SecretUnlockReadDto

interface SecretUnlockRemoteDataSource {
    suspend fun unlock(pin: String): SecretUnlockReadDto

    /** The unlock to renew goes in the header, put there by `sendSecretUnlock`. */
    suspend fun renew(): SecretUnlockReadDto
}
