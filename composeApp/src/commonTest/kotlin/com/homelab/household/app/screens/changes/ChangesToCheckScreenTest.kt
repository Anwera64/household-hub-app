package com.homelab.household.app.screens.changes

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.homelab.household.app.testing.FakeMembersHub
import com.homelab.household.app.testing.runScreenTest
import com.homelab.household.presentation.memories.ChangeStatus
import com.homelab.household.presentation.memories.ChangesToCheckUiState
import kotlin.test.Test
import kotlin.test.assertNull

/** Changes to check, with the real stack under it; only the hub is faked. */
@OptIn(ExperimentalTestApi::class)
class ChangesToCheckScreenTest {
    private val wait = 5_000L

    private val stored = "Is vegetarian, but cooks fish for Liam."
    private val read = "Eats fish again since September."

    private fun hubWithAChange() = FakeMembersHub().apply { readsAChange("c1", stored = stored, read = read) }

    @Test
    fun `GIVEN a change WHEN the page opens THEN it shows Stored beside what was read the chat and the three answers`() {
        runScreenTest {
            changesToCheckScreen(hubWithAChange())

            onChangesToCheck {
                seesTheTitle(count = 1)
                seesTheChange(stored = stored, read = read)
                seesItWasReadIn("Dinner ideas")
                seesTheThreeAnswers()
            }
        }
    }

    @Test
    fun `GIVEN a change WHEN Use new is tapped THEN it turns into Replaced with Undo and Undo opens it again`() {
        val hub = hubWithAChange()

        runScreenTest {
            changesToCheckScreen(hub)

            onChangesToCheck {
                usesNew()
                seesItReplaced(stored)
                seesNothingLeft()
            }
            waitUntil(timeoutMillis = wait) { hub.answerTo("c1") == "use_new" }

            onChangesToCheck {
                undoes()
                seesTheThreeAnswers()
                seesTheTitle(count = 1)
            }
            waitUntil(timeoutMillis = wait) { hub.answerTo("c1") == null }
        }
    }

    @Test
    fun `GIVEN the hub won't take answers WHEN Keep stored is tapped THEN the card stays open and says nothing changed`() {
        val hub = hubWithAChange().apply { refusesAnswers() }

        runScreenTest {
            changesToCheckScreen(hub)

            onChangesToCheck {
                keepsStored()
                seesTheAnswerFailed()
                seesTheThreeAnswers()
            }
            assertNull(hub.answerTo("c1"))
        }
    }

    @Test
    fun `GIVEN a change about Liam's memory from your chat WHEN the page opens THEN it waits for Liam with no answers`() {
        val hub =
            FakeMembersHub().apply {
                readsAChange(
                    "c2",
                    stored = "Works late on Thursdays.",
                    read = "Now works late on Fridays.",
                    mine = false,
                )
            }

        runScreenTest {
            changesToCheckScreen(hub)

            onChangesToCheck {
                seesNothingLeft()
                seesItWaitsFor("Liam")
                seesTheChange(stored = "Works late on Thursdays.", read = "Now works late on Fridays.")
                seesNoAnswers()
            }
        }
    }

    @Test
    fun `GIVEN a change read in someone else's chat WHEN the page opens THEN it names whose chat never its title`() {
        val hub = FakeMembersHub().apply { readsAChange("c3", stored = stored, read = read, title = null) }

        runScreenTest {
            changesToCheckScreen(hub)

            onChangesToCheck { seesItWasReadInTheChatOf("Liam") }
        }
    }

    @Test
    fun `GIVEN the hub is down WHEN the page opens THEN it says it can't reach the hub`() {
        runScreenTest {
            changesToCheckScreen(hubWithAChange().apply { isOffline() })

            onChangesToCheck { seesTheHubCannotBeReached() }
        }
    }

    @Test
    fun `GIVEN an answer saving WHEN the page draws THEN the tapped button says Saving`() {
        val state =
            ChangesToCheckUiStateProvider().values.first { state ->
                state.toCheck.any { it.status is ChangeStatus.Saving }
            }

        runComposeUiTest {
            changesToCheckContent(state)

            onChangesToCheck { seesItSaving() }
        }
    }

    private val dentist = "Sam has a dentist appointment on Monday at 09:00."
    private val shop = "The household does the big shop on Saturday mornings."

    @Test
    fun `GIVEN a fact not saved yet and a change WHEN the page opens THEN one heading counts both and the fact comes first`() {
        val hub = hubWithAChange().apply { holdsAFact("h1", dentist) }

        runScreenTest {
            changesToCheckScreen(hub)

            onChangesToCheck {
                seesTheHeadingForAll(count = 2)
                seesFactsNotSavedYetBeforeChanges(held = 1, changes = 1)
            }
        }
    }

    @Test
    fun `GIVEN a personal fact with an end day WHEN the page opens THEN its card shows Yours and until and the rule and both answers`() {
        val hub = FakeMembersHub().apply { holdsAFact("h1", dentist, expiresAt = "2026-12-23T10:00:00Z") }

        runScreenTest {
            changesToCheckScreen(hub)

            onChangesToCheck {
                seesAFactNotSavedYet(dentist)
                seesItIsYours()
                seesItGoesInDecemberOn(day = 23)
                seesNothingAboutSharing()
            }
        }
    }

    @Test
    fun `GIVEN a household fact WHEN the page opens THEN its card says saving shares it`() {
        val hub = FakeMembersHub().apply { holdsAFact("h2", shop, household = true) }

        runScreenTest {
            changesToCheckScreen(hub)

            onChangesToCheck {
                seesAFactNotSavedYet(shop)
                seesItIsTheHousehold()
                seesSavingSharesIt()
            }
        }
    }

    @Test
    fun `GIVEN a fact not saved yet WHEN Save is tapped THEN it says Saved with Undo and Undo opens it again`() {
        val hub = FakeMembersHub().apply { holdsAFact("h1", dentist) }

        runScreenTest {
            changesToCheckScreen(hub)

            onChangesToCheck {
                saves()
                seesItSaved(dentist)
                seesNothingLeft()
            }
            waitUntil(timeoutMillis = wait) { hub.heldAnswerTo("h1") == "save" }

            onChangesToCheck {
                undoes()
                seesAFactNotSavedYet(dentist)
            }
            waitUntil(timeoutMillis = wait) { hub.heldAnswerTo("h1") == null }
        }
    }

    @Test
    fun `GIVEN a fact not saved yet WHEN Don't save is tapped THEN it says Not saved`() {
        val hub = FakeMembersHub().apply { holdsAFact("h1", dentist) }

        runScreenTest {
            changesToCheckScreen(hub)

            onChangesToCheck {
                doesNotSave()
                seesItNotSaved(dentist)
            }
            waitUntil(timeoutMillis = wait) { hub.heldAnswerTo("h1") == "dont_save" }
        }
    }

    @Test
    fun `GIVEN the hub won't take answers WHEN Save is tapped on a fact THEN the card stays open and says nothing changed`() {
        val hub =
            FakeMembersHub().apply {
                holdsAFact("h1", dentist)
                refusesHeldAnswers()
            }

        runScreenTest {
            changesToCheckScreen(hub)

            onChangesToCheck {
                saves()
                seesTheAnswerFailed()
                seesAFactNotSavedYet(dentist)
            }
            assertNull(hub.heldAnswerTo("h1"))
        }
    }

    @Test
    fun `GIVEN a fact answered on another phone WHEN it is answered here THEN the page is read again without it`() {
        val hub =
            FakeMembersHub().apply {
                holdsAFact("h1", dentist)
                heldFactsAreGone()
            }

        runScreenTest {
            changesToCheckScreen(hub)

            onChangesToCheck {
                saves()
                seesNothingLeft()
                doesNotSee(dentist)
            }
        }
    }

    @Test
    fun `GIVEN only facts not saved yet WHEN the page opens THEN the heading counts them and there is no Changes label`() {
        val hub = FakeMembersHub().apply { holdsAFact("h1", dentist) }

        runScreenTest {
            changesToCheckScreen(hub)

            onChangesToCheck {
                seesTheHeadingForAll(count = 1)
                seesAFactNotSavedYet(dentist)
                doesNotSeeTheChangesLabel()
            }
        }
    }

    @Test
    fun `GIVEN no facts not saved yet WHEN the page opens THEN it reads as it always has`() {
        runScreenTest {
            changesToCheckScreen(hubWithAChange())

            onChangesToCheck {
                seesTheTitle(count = 1)
                seesTheChangesIntro()
                seesTheChange(stored = stored, read = read)
                seesNoSectionLabels()
            }
        }
    }

    @Test
    fun `GIVEN every previewed state WHEN it is drawn THEN it settles without failing`() {
        ChangesToCheckUiStateProvider().values.forEach { state ->
            runComposeUiTest {
                changesToCheckContent(state)
                waitForIdle()
            }
        }
    }

    @Test
    fun `GIVEN Liam took his sentence back WHEN Use new is tapped THEN the card says so and nothing is left to check`() {
        val hub = hubWithAChange().apply { changeIsTakenBackBy("Liam") }

        runScreenTest {
            changesToCheckScreen(hub)

            onChangesToCheck {
                usesNew()
                seesItWasTakenBack("Liam took this back.")
                seesNothingLeft()
            }
        }
    }
}
