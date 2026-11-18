---
name: test-on-emulator
description: Run a PR's manual test yourself on any Android emulator, with one video per step, and report what passed
---
Given a PR number (none given: the PR of the current branch). Use this when I ask you to test it yourself; `test-pr` is for my phone. Helpers: `scripts/emu.sh` (run it with no arguments for its commands) and `scripts/sheets.py`.

1. **Prepare the PR** as in `test-pr` steps 1–3: rebase, `ktlintCheck jvmTest`, and the backend change on the dev hub (check the dev hub's code, not just master).
2. **Model first.** `emu.sh model`. On the PC it starts Docker and the model with `hosts/desktop/startup_homelab.ps1` when Ollama doesn't answer. If it still doesn't answer, stop and tell me: a test without replies misses streaming and withheld answers.
3. **Emulator.** `emu.sh pick` uses any running emulator, or boots the first AVD. Never a physical phone. Install with `ANDROID_SERIAL` set to that serial (`.\gradlew.bat :androidApp:installDebug`, `JAVA_HOME` as in the Setup wiki page) and launch the dev app.
4. **Sign in.** Handle the local-network screen and the profile picker with `tap-text`. Ask me for the dev PIN; never write it into a file, commit or message.
5. **Run the issue's manual test, one video per step.** `emu.sh rec stepN-what-it-checks` before the step, `emu.sh stop` after it. Keep each video under 3 minutes and re-record a botched take rather than keep it.
   - Tap by text (`tap-text`), not by pixels; read the screen with `ui`.
   - Wait with `wait-for TEXT`, never a bare sleep.
   - Watch the app's log lines with a Monitor running `emu.sh logwatch`. React to its network and error lines (a 423, a re-read, a crash) rather than to time.
   - Use `back` for system Back: it closes the keyboard first, so it never sends one Back too many.
   - `lock` locks and wakes the emulator.
6. **Cross-check** each step with the hub's logs in Loki and a read-only query on the dev hub's database (`mode=ro`). Decline every write card.
7. **Review the videos.** `python sheets.py sheet <video dir> <out dir>`, read each sheet, then `sheets.py frames` around every transition: opening a chat, the lock, Send, the pad. A dump taken seconds later misses what flashes for half a second.
8. **Report** one table: step, what I should see, what the video shows, pass/fail. Then list anything odd the frames show, each with its video and timestamp. Send the videos (in the app repo's gitignored `out/`).
9. **Leave things as found:** the app repo back on its branch, the emulator running. Present a plan before coding any fix.
