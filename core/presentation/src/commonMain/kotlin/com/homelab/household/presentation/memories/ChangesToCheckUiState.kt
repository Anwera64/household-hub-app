package com.homelab.household.presentation.memories

/**
 * Changes to check (canvas MemProposals, MemHeldProposals): facts not saved yet first, then your
 * cards to answer, then the ones waiting for someone else.
 */
data class ChangesToCheckUiState(
    val status: MemoriesStatus = MemoriesStatus.Loading,
    val toCheck: List<ChangeRow> = emptyList(),
    val waiting: List<ChangeRow> = emptyList(),
    val held: List<HeldRow> = emptyList(),
) {
    /** "2 to check", changes and facts alike; at 0 the page says "Nothing left to check". */
    val openCount: Int get() = changesOpenCount + heldOpenCount

    /** "Not saved yet · 1". */
    val heldOpenCount: Int get() = held.openCount()

    /** "Changes · 1". */
    val changesOpenCount: Int get() = toCheck.openCount()

    val waitingByAnswerer: List<Pair<String, List<ChangeRow>>> get() = waiting.byAnswerer()
}
