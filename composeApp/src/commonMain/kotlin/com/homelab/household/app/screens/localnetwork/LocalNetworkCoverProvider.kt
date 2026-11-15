package com.homelab.household.app.screens.localnetwork

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.homelab.household.app.platform.SettingsPlatform

/** Every board of the local network flow: first time, off on each phone, 2b, checking and still off. */
class LocalNetworkCoverProvider : PreviewParameterProvider<LocalNetworkCover> {
    override val values: Sequence<LocalNetworkCover> =
        sequenceOf(
            LocalNetworkCover(LocalNetworkCover.Step.FirstTime, SettingsPlatform.Ios),
            LocalNetworkCover(LocalNetworkCover.Step.Off, SettingsPlatform.Ios),
            LocalNetworkCover(LocalNetworkCover.Step.Off, SettingsPlatform.Android),
            LocalNetworkCover(LocalNetworkCover.Step.Off, SettingsPlatform.Android, canAskAgain = true),
            LocalNetworkCover(LocalNetworkCover.Step.Checking, SettingsPlatform.Ios),
            LocalNetworkCover(LocalNetworkCover.Step.StillOff, SettingsPlatform.Android),
        )

    override fun getDisplayName(index: Int): String =
        values.elementAt(index).let { cover ->
            "${cover.step} · ${cover.platform}" + if (cover.canAskAgain) " · can ask again" else ""
        }
}
