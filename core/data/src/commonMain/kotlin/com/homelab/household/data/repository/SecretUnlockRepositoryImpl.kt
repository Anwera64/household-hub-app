package com.homelab.household.data.repository

import com.homelab.household.data.datasource.local.SecretUnlockLocalDataSource
import com.homelab.household.data.datasource.remote.`interface`.SecretUnlockRemoteDataSource
import com.homelab.household.domain.repository.SecretUnlockRepository
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

class SecretUnlockRepositoryImpl(
    private val remote: SecretUnlockRemoteDataSource,
    private val local: SecretUnlockLocalDataSource,
) : SecretUnlockRepository {
    override val unlocked: StateFlow<Boolean> = local.unlocked

    override suspend fun unlock(pin: String) {
        local.hold(remote.unlock(pin).unlock_token)
    }

    /** A 423 here has already made the header plugin forget the unlock it sent. */
    override suspend fun renew() {
        local.hold(remote.renew().unlock_token)
    }

    override fun lock() = local.forgetAll()

    override fun holdChatUnlock(
        sessionId: String,
        token: String,
    ) = local.holdChat(sessionId, token)

    override fun tokenAge(): Duration? = local.age()
}
