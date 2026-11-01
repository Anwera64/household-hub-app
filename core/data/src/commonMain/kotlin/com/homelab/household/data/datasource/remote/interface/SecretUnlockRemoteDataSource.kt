package com.homelab.household.data.datasource.remote.`interface`

import com.homelab.household.data.dto.SecretUnlockReadDto

interface SecretUnlockRemoteDataSource {
    suspend fun unlock(pin: String): SecretUnlockReadDto

    /** A fresh unlock of the same kind as [token]: every chat, or that one chat. */
    suspend fun renew(token: String): SecretUnlockReadDto
}
