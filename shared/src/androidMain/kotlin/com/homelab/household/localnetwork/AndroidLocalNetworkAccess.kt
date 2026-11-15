package com.homelab.household.localnetwork

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.homelab.household.domain.localnetwork.LocalNetworkAccess
import com.homelab.household.domain.localnetwork.LocalNetworkStatus

/**
 * Android 17 blocks local network traffic for apps targeting it until `ACCESS_LOCAL_NETWORK` is
 * granted (developer.android.com/privacy-and-security/local-network-permission). A blocked
 * connection times out rather than failing fast. Reading the permission never asks the member.
 */
class AndroidLocalNetworkAccess(
    private val context: Context,
) : LocalNetworkAccess {
    override val readsWithoutAsking: Boolean = true

    override suspend fun status(): LocalNetworkStatus =
        when {
            Build.VERSION.SDK_INT < ANDROID_17 || context.applicationInfo.targetSdkVersion < ANDROID_17 -> {
                LocalNetworkStatus.NotNeeded
            }

            context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED -> {
                LocalNetworkStatus.Allowed
            }

            else -> {
                LocalNetworkStatus.Off
            }
        }

    companion object {
        const val PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

        /** API level 37, where the permission is enforced. */
        private const val ANDROID_17 = 37
    }
}
