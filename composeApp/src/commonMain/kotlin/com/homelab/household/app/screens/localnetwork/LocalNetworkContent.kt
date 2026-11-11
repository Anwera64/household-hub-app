package com.homelab.household.app.screens.localnetwork

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.homelab.household.app.components.BentoCard
import com.homelab.household.app.components.ChipVariant
import com.homelab.household.app.components.HearthChip
import com.homelab.household.app.components.HearthScaffold
import com.homelab.household.app.components.PrimaryButton
import com.homelab.household.app.components.QuietTextButton
import com.homelab.household.app.components.SecondaryButton
import com.homelab.household.app.icons.HearthIcon
import com.homelab.household.app.icons.HearthIconImage
import com.homelab.household.app.platform.SettingsPlatform
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.app_name
import com.homelab.household.app.resources.local_network_away
import com.homelab.household.app.resources.local_network_away_first
import com.homelab.household.app.resources.local_network_away_off
import com.homelab.household.app.resources.local_network_checking
import com.homelab.household.app.resources.local_network_chip_checking
import com.homelab.household.app.resources.local_network_chip_needs_ok
import com.homelab.household.app.resources.local_network_chip_off
import com.homelab.household.app.resources.local_network_chip_still_off
import com.homelab.household.app.resources.local_network_chip_works
import com.homelab.household.app.resources.local_network_continue
import com.homelab.household.app.resources.local_network_first_body
import com.homelab.household.app.resources.local_network_first_hint
import com.homelab.household.app.resources.local_network_first_title
import com.homelab.household.app.resources.local_network_home
import com.homelab.household.app.resources.local_network_home_first
import com.homelab.household.app.resources.local_network_home_off
import com.homelab.household.app.resources.local_network_off_body
import com.homelab.household.app.resources.local_network_off_title
import com.homelab.household.app.resources.local_network_open_settings
import com.homelab.household.app.resources.local_network_still_off_body
import com.homelab.household.app.resources.local_network_turn_on
import com.homelab.household.app.resources.local_network_turn_on_body
import com.homelab.household.app.resources.local_network_where
import com.homelab.household.app.resources.local_network_where_android
import com.homelab.household.app.resources.local_network_where_android_spoken
import com.homelab.household.app.resources.local_network_where_ios
import com.homelab.household.app.resources.secret_off_dont_ask
import com.homelab.household.app.resources.secret_offer_not_now
import com.homelab.household.app.theme.HearthShapes
import com.homelab.household.app.theme.HearthTheme
import com.homelab.household.app.theme.PreviewDayNight
import org.jetbrains.compose.resources.stringResource

/** Which of the local network screens to draw, and how the phone it runs on differs. */
data class LocalNetworkCover(
    val step: Step,
    val platform: SettingsPlatform,
    /** Android after a single "Don't allow": its own question can still be shown (board 2b). */
    val canAskAgain: Boolean = false,
) {
    enum class Step { FirstTime, Off, Checking, StillOff }
}

/**
 * The explanation before the phone's own question (1), and the cover saying local network access is
 * off (2, 2b, 3, 3a). Tone is attention, not error: it is a setting to fix, not a failure. Top-aligned,
 * and it scrolls when it doesn't fit, for a small phone or large text.
 *
 * Stateless: [LocalNetworkGateHost] reads the gate and hands the step down.
 */
@Composable
fun LocalNetworkContent(
    cover: LocalNetworkCover,
    onContinue: () -> Unit,
    onOpenSettings: () -> Unit,
    onTurnOn: () -> Unit,
    onNotNow: () -> Unit,
    onDontAskAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appName = stringResource(Res.string.app_name)
    val firstTime = cover.step == LocalNetworkCover.Step.FirstTime
    // The phone can still ask only from the plain "access is off": once back from Settings, the way
    // on is Settings.
    val asksAgain = cover.step == LocalNetworkCover.Step.Off && cover.canAskAgain

    HearthScaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(top = HearthTheme.spacing.huge, bottom = HearthTheme.spacing.xxxl),
        ) {
            Header(
                firstTime = firstTime,
                title =
                    stringResource(
                        if (firstTime) Res.string.local_network_first_title else Res.string.local_network_off_title,
                    ),
                body = bodyFor(cover.step, asksAgain, appName),
            )
            Spacer(Modifier.height(HearthTheme.spacing.xxl))
            Column(verticalArrangement = Arrangement.spacedBy(HearthTheme.spacing.lg)) {
                StatusCard(cover.step)
                if (!firstTime && !asksAgain) WhereCard(cover.platform, appName)
            }
            Spacer(
                Modifier
                    .weight(1f)
                    .heightIn(min = HearthTheme.spacing.xxl),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(HearthTheme.spacing.md),
            ) {
                if (firstTime) {
                    Text(
                        text = stringResource(Res.string.local_network_first_hint),
                        style = HearthTheme.typography.caption,
                        color = HearthTheme.colors.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PrimaryButton(
                        text = stringResource(Res.string.local_network_continue),
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    if (asksAgain) {
                        PrimaryButton(
                            text = stringResource(Res.string.local_network_turn_on),
                            onClick = onTurnOn,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        val checking = cover.step == LocalNetworkCover.Step.Checking
                        PrimaryButton(
                            text =
                                stringResource(
                                    if (checking) {
                                        Res.string.local_network_checking
                                    } else {
                                        Res.string.local_network_open_settings
                                    },
                                ),
                            onClick = onOpenSettings,
                            icon = if (checking) null else HearthIcon.Settings,
                            busy = checking,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    SecondaryButton(
                        text = stringResource(Res.string.secret_offer_not_now),
                        onClick = onNotNow,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    QuietTextButton(
                        text = stringResource(Res.string.secret_off_dont_ask),
                        onClick = onDontAskAgain,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun bodyFor(
    step: LocalNetworkCover.Step,
    asksAgain: Boolean,
    appName: String,
): String =
    when {
        step == LocalNetworkCover.Step.FirstTime -> stringResource(Res.string.local_network_first_body, appName)
        step == LocalNetworkCover.Step.StillOff -> stringResource(Res.string.local_network_still_off_body, appName)
        asksAgain -> stringResource(Res.string.local_network_turn_on_body, appName)
        else -> stringResource(Res.string.local_network_off_body, appName)
    }

@Composable
private fun Header(
    firstTime: Boolean,
    title: String,
    body: String,
) {
    val colors = HearthTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(HearthTheme.spacing.lg)) {
        Tile(
            icon = if (firstTime) HearthIcon.HubOnline else HearthIcon.HubOffline,
            container = if (firstTime) colors.primaryContainer else colors.tertiaryContainer,
            content = if (firstTime) colors.onPrimaryContainer else colors.onTertiaryContainer,
        )
        Text(
            text = title,
            style = HearthTheme.typography.hero,
            color = colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = body, style = HearthTheme.typography.bodyLarge, color = colors.textMuted)
    }
}

@Composable
private fun Tile(
    icon: HearthIcon,
    container: Color,
    content: Color,
) {
    Box(
        modifier = Modifier
            .size(HearthTheme.size.tile)
            .background(color = container, shape = HearthShapes.tile),
        contentAlignment = Alignment.Center,
    ) {
        HearthIconImage(icon = icon, contentDescription = null, size = HearthTheme.size.iconXxl, tint = content)
    }
}

/** At home on Wi-Fi, and away through Tailscale: what access changes, and what it doesn't. */
@Composable
private fun StatusCard(step: LocalNetworkCover.Step) {
    val firstTime = step == LocalNetworkCover.Step.FirstTime
    BentoCard(modifier = Modifier.fillMaxWidth()) {
        StatusRow(
            icon = HearthIcon.Household,
            label = stringResource(Res.string.local_network_home),
            caption =
                stringResource(
                    if (firstTime) Res.string.local_network_home_first else Res.string.local_network_home_off,
                ),
            chip =
                stringResource(
                    when (step) {
                        LocalNetworkCover.Step.FirstTime -> Res.string.local_network_chip_needs_ok
                        LocalNetworkCover.Step.Off -> Res.string.local_network_chip_off
                        LocalNetworkCover.Step.Checking -> Res.string.local_network_chip_checking
                        LocalNetworkCover.Step.StillOff -> Res.string.local_network_chip_still_off
                    },
                ),
            chipVariant =
                when (step) {
                    LocalNetworkCover.Step.FirstTime, LocalNetworkCover.Step.Checking -> ChipVariant.Neutral
                    LocalNetworkCover.Step.Off, LocalNetworkCover.Step.StillOff -> ChipVariant.Attention
                },
        )
        HorizontalDivider(thickness = HearthTheme.size.hairline, color = HearthTheme.colors.outlineSoft)
        StatusRow(
            icon = HearthIcon.Globe,
            label = stringResource(Res.string.local_network_away),
            caption =
                stringResource(
                    if (firstTime) Res.string.local_network_away_first else Res.string.local_network_away_off,
                ),
            chip = stringResource(Res.string.local_network_chip_works),
            chipVariant = ChipVariant.Success,
        )
    }
}

/** One merged node, so a screen reader reads the row as one line. */
@Composable
private fun StatusRow(
    icon: HearthIcon,
    label: String,
    caption: String,
    chip: String,
    chipVariant: ChipVariant,
) {
    val colors = HearthTheme.colors
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HearthTheme.spacing.md),
    ) {
        HearthIconImage(
            icon = icon,
            contentDescription = null,
            size = HearthTheme.size.iconLg,
            tint = colors.textPrimary,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(HearthTheme.spacing.xxs),
        ) {
            Text(label, style = HearthTheme.typography.bodyStrong, color = colors.textPrimary)
            Text(caption, style = HearthTheme.typography.caption, color = colors.textMuted)
        }
        HearthChip(label = chip, variant = chipVariant)
    }
}

/** The one line that differs per phone. */
@Composable
private fun WhereCard(
    platform: SettingsPlatform,
    appName: String,
) {
    val colors = HearthTheme.colors
    BentoCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(HearthTheme.spacing.md)) {
            HearthIconImage(
                icon = HearthIcon.Settings,
                contentDescription = null,
                size = HearthTheme.size.iconLg,
                tint = colors.textPrimary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(HearthTheme.spacing.xxs)) {
                Text(
                    stringResource(Res.string.local_network_where),
                    style = HearthTheme.typography.bodyStrong,
                    color = colors.textPrimary,
                )
                WherePath(platform, appName, colors.textMuted)
            }
        }
    }
}

/**
 * Where the switch is in the phone's Settings. Android's path is read out step by step, since a
 * screen reader would otherwise read the arrows as symbols.
 */
@Composable
fun WherePath(
    platform: SettingsPlatform,
    appName: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    when (platform) {
        SettingsPlatform.Ios -> {
            Text(
                stringResource(Res.string.local_network_where_ios, appName),
                modifier = modifier,
                style = HearthTheme.typography.caption,
                color = color,
            )
        }

        SettingsPlatform.Android -> {
            val spoken = stringResource(Res.string.local_network_where_android_spoken, appName)
            Text(
                stringResource(Res.string.local_network_where_android, appName),
                modifier = modifier.semantics { contentDescription = spoken },
                style = HearthTheme.typography.caption,
                color = color,
            )
        }
    }
}

@PreviewDayNight
@Composable
private fun LocalNetworkContentPreview(
    @PreviewParameter(LocalNetworkCoverProvider::class) cover: LocalNetworkCover,
) {
    HearthTheme {
        LocalNetworkContent(
            cover = cover,
            onContinue = {},
            onOpenSettings = {},
            onTurnOn = {},
            onNotNow = {},
            onDontAskAgain = {},
        )
    }
}
