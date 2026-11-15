package com.homelab.household.data.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpResponseValidator

/**
 * Tells [onUnreachable] when a call to the hub finds no route: the failures
 * [NetworkExceptionHelper.isNetworkOfflineException] counts as offline, such as a connect timeout.
 * On Android 17 a phone without local network access sees exactly that at home. A hub that answers,
 * with any status, is not unreachable. The failure itself still reaches the caller unchanged.
 */
fun HttpClientConfig<*>.reportUnreachable(onUnreachable: () -> Unit) {
    HttpResponseValidator {
        handleResponseExceptionWithRequest { cause, _ ->
            if (NetworkExceptionHelper.isNetworkOfflineException(cause)) onUnreachable()
        }
    }
}
