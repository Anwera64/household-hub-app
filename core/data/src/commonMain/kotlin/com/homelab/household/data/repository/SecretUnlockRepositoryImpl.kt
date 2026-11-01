package com.homelab.household.data.repository

import com.homelab.household.data.datasource.local.SecretUnlockLocalDataSource
import com.homelab.household.data.datasource.remote.`interface`.SecretUnlockRemoteDataSource
import com.homelab.household.domain.repository.SecretUnlockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

class SecretUnlockRepositoryImpl(
    private val remote: SecretUnlockRemoteDataSource,
    private val local: SecretUnlockLocalDataSource,
) : SecretUnlockRepository {
    override val holding: StateFlow<Boolean> = local.holding

    override val locks: Flow<Unit> = local.locks

    override suspend fun unlock(pin: String) {
        local.hold(remote.unlock(pin).unlock_token)
    }

    /**
     * Every unlock held, renewed to its own kind: the hub renews a chat's own unlock to one for that
     * chat alone. A 423 throws `SecretLockedException`, and the header plugin has forgotten that one.
     */
    override suspend fun renew() {
        local.heldTokens().forEach { (chat, token) ->
            val fresh = remote.renew(token).unlock_token
            if (chat == null) local.hold(fresh) else local.holdChat(chat, fresh)
        }
    }

    override fun lock() = local.forgetAll()

    override fun holdChatUnlock(
        sessionId: String,
        token: String,
    ) = local.holdChat(sessionId, token)

    override fun tokenAge(): Duration? = local.age()
}
