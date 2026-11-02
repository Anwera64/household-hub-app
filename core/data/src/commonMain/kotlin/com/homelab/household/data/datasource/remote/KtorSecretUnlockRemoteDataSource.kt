package com.homelab.household.data.datasource.remote

import com.homelab.household.data.datasource.remote.`interface`.SecretUnlockRemoteDataSource
import com.homelab.household.data.dto.SecretUnlockReadDto
import com.homelab.household.data.dto.UnlockSecretRequestDto
import com.homelab.household.data.network.SECRET_UNLOCK_HEADER
import com.homelab.household.data.network.ensureJsonSuccess
import com.homelab.household.data.network.reachingHub
import com.homelab.household.data.network.throwIfPinRefused
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class KtorSecretUnlockRemoteDataSource(
    private val client: HttpClient,
    private val baseUrl: String,
) : SecretUnlockRemoteDataSource {
    override suspend fun unlock(pin: String): SecretUnlockReadDto =
        reachingHub {
            val response =
                client.post("$baseUrl/api/v1/auth/unlock-secret") {
                    contentType(ContentType.Application.Json)
                    setBody(UnlockSecretRequestDto(pin = pin))
                }
            response.throwIfPinRefused()
            response.ensureJsonSuccess().body()
        }

    override suspend fun renew(token: String): SecretUnlockReadDto =
        reachingHub {
            client
                .post("$baseUrl/api/v1/auth/unlock-secret/renew") { header(SECRET_UNLOCK_HEADER, token) }
                .ensureJsonSuccess()
                .body()
        }
}
