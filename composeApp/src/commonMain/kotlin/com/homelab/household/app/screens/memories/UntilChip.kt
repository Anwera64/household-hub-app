package com.homelab.household.app.screens.memories

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.homelab.household.app.components.ChipVariant
import com.homelab.household.app.components.HearthChip
import com.homelab.household.app.icons.HearthIcon
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.memories_until
import com.homelab.household.presentation.memories.LocalDay
import org.jetbrains.compose.resources.stringResource

/** "until 23 Nov": the day a memory, or a fact not saved yet, goes on its own. */
@Composable
internal fun UntilChip(
    until: LocalDay,
    modifier: Modifier = Modifier,
) {
    HearthChip(
        label = stringResource(Res.string.memories_until, until.day, stringResource(SHORT_MONTHS[until.month - 1])),
        modifier = modifier,
        variant = ChipVariant.Attention,
        icon = HearthIcon.Schedule,
    )
}
