package com.homelab.household.app.screens.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.homelab.household.app.components.BentoCard
import com.homelab.household.app.components.ChipVariant
import com.homelab.household.app.components.HearthChip
import com.homelab.household.app.components.SecondaryButton
import com.homelab.household.app.resources.Res
import com.homelab.household.app.resources.changes_failed
import com.homelab.household.app.resources.changes_saving
import com.homelab.household.app.resources.held_done_not_saved
import com.homelab.household.app.resources.held_done_saved
import com.homelab.household.app.resources.held_dont_save
import com.homelab.household.app.resources.held_household
import com.homelab.household.app.resources.held_label
import com.homelab.household.app.resources.held_rule
import com.homelab.household.app.resources.held_save
import com.homelab.household.app.resources.memories_tab_household_plain
import com.homelab.household.app.resources.memories_tab_yours_plain
import com.homelab.household.app.screens.memories.UntilChip
import com.homelab.household.app.theme.HearthShapes
import com.homelab.household.app.theme.HearthTheme
import com.homelab.household.domain.model.HeldFactAnswer
import com.homelab.household.domain.model.MemoryScope
import com.homelab.household.presentation.memories.HeldRow
import com.homelab.household.presentation.memories.HeldStatus
import org.jetbrains.compose.resources.stringResource

/**
 * A fact not saved yet (canvas MemHeldProposals, MemHeldAnswered): the sentence an agent read in a
 * chat holding a private message, the rule for answering, and Don't save · Save, neither one nudged.
 * [showSource] is off in the chat sheet, where the chat is the one already open. Once answered it
 * becomes a short row with Undo, as a change card does.
 */
@Composable
internal fun HeldCard(
    row: HeldRow,
    onAnswer: (HeldFactAnswer) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    showSource: Boolean = true,
) {
    when (val status = row.status) {
        is HeldStatus.Done -> HeldDoneRow(row = row, done = status, onUndo = onUndo, modifier = modifier)
        else -> HeldOpenCard(row = row, onAnswer = onAnswer, showSource = showSource, modifier = modifier)
    }
}

@Composable
private fun HeldOpenCard(
    row: HeldRow,
    onAnswer: (HeldFactAnswer) -> Unit,
    showSource: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = HearthTheme.colors
    val type = HearthTheme.typography
    val fact = row.fact
    val household = fact.scope == MemoryScope.HOUSEHOLD
    BentoCard(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(HearthTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(HearthTheme.spacing.xs),
        ) {
            HearthChip(
                label =
                    stringResource(
                        if (household) Res.string.memories_tab_household_plain else Res.string.memories_tab_yours_plain,
                    ),
                variant = if (household) ChipVariant.Neutral else ChipVariant.Primary,
            )
            row.until?.let { until -> UntilChip(until) }
        }
        SentenceBox(
            label = stringResource(Res.string.held_label),
            sentence = fact.content,
            meta =
                if (showSource) {
                    sourceWords(fact.sourceTitle, fact.sourceOwnerName, row.readAge, row.readTime)
                } else {
                    null
                },
            container = colors.surfaceAlt,
            ink = colors.textPrimary,
        )
        Text(stringResource(Res.string.held_rule), style = type.caption, color = colors.textMuted)
        if (household) {
            Text(
                stringResource(Res.string.held_household),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(HearthShapes.item)
                    .background(colors.tertiaryContainer)
                    .padding(horizontal = HearthTheme.spacing.md, vertical = HearthTheme.spacing.sm),
                style = type.caption,
                color = colors.onTertiaryContainer,
            )
        }
        val status = row.status
        if (status is HeldStatus.Open && status.failed) {
            Text(
                stringResource(Res.string.changes_failed),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.caption,
                color = colors.error,
            )
        }
        HeldAnswerButtons(saving = (status as? HeldStatus.Saving)?.answer, onAnswer = onAnswer)
    }
}

/** Don't save · Save, both outlined and of equal width. The tapped one says "Saving…". */
@Composable
private fun HeldAnswerButtons(
    saving: HeldFactAnswer?,
    onAnswer: (HeldFactAnswer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val savingWords = stringResource(Res.string.changes_saving)
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(HearthTheme.spacing.sm)) {
        listOf(
            HeldFactAnswer.DontSave to Res.string.held_dont_save,
            HeldFactAnswer.Save to Res.string.held_save,
        ).forEach { (answer, words) ->
            SecondaryButton(
                text = if (saving == answer) savingWords else stringResource(words),
                onClick = { onAnswer(answer) },
                modifier = Modifier.weight(1f),
                busy = saving == answer,
                compact = true,
            )
        }
    }
}

/** After an answer: "Saved “…”" or "Not saved: “…”", with Undo, until the page is left. */
@Composable
private fun HeldDoneRow(
    row: HeldRow,
    done: HeldStatus.Done,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val words =
        when (done.answer) {
            HeldFactAnswer.Save -> stringResource(Res.string.held_done_saved, row.fact.content)
            HeldFactAnswer.DontSave -> stringResource(Res.string.held_done_not_saved, row.fact.content)
        }
    DoneRow(words = words, undoFailed = done.undoFailed, onUndo = onUndo, modifier = modifier)
}
