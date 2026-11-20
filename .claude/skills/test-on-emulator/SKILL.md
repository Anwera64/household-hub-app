---
name: test-on-emulator
description: Run a PR's manual test yourself on any Android emulator, with one video per step, and report each scenario with its evidence
---
Given a PR number (none given: the PR of the current branch). Use this when I ask you to test it yourself; `test-pr` is for my phone. Helpers in `scripts/`: `emu.sh` (run it with no arguments for its commands), `sheets.py` (frames and report media, needs opencv-python) and `report.mjs` (the report page).

1. **Prepare the PR** as in `test-pr` steps 1–3: rebase, `ktlintCheck jvmTest`, and the backend change on the dev hub (check the dev hub's code, not just master). Note any required CI check that didn't pass: a cancelled one blocks the merge as much as a failed one.
2. **Model first.** `emu.sh model`. On the PC it starts Docker and the model with `hosts/desktop/startup_homelab.ps1` when Ollama doesn't answer. If it still doesn't answer, stop and tell me: a test without replies misses streaming and withheld answers.
3. **Emulator.** `emu.sh pick` uses any running emulator, or boots the first AVD. Never a physical phone. Install with `ANDROID_SERIAL` set to that serial (`.\gradlew.bat :androidApp:installDebug`, `JAVA_HOME` as in the Setup wiki page) and launch the dev app. Reinstalling keeps the sign-in.
4. **Sign in.** Handle the local-network screen and the profile picker with `tap-text`, then `emu.sh pin`, which types the dev PIN from `HUB_DEV_PIN` on this PC. If it says the variable isn't set, ask me. Never write the PIN into the repo, a file, a commit or a message.
5. **Test data.** Rows the test needs go into the dev hub's database with a throwaway script, with fixed IDs you can spot (`00000000-<issue>-…`). Note each write for the report.
6. **Run the issue's manual test, one video per step.** `emu.sh rec stepN-what-it-checks` before the step, `emu.sh stop` after it. Keep each video under 3 minutes and re-record a botched take rather than keep it.
   - Tap by text (`tap-text`), not by pixels; read the screen with `ui`.
   - Wait with `wait-for TEXT`, never a bare sleep. It ignores case: labels dump in capitals.
   - Watch the app's log lines with a Monitor running `emu.sh logwatch`. React to its network and error lines (a 423, a re-read, a crash) rather than to time. Every line, with its time, also goes to `applog.txt`. `emu.sh logstop` when you're done.
   - Never time anything from a video: the recorder can hold a frame until the next UI dump. Use `emu.sh burst` right after the action, and the times in `applog.txt`.
   - Use `back` for system Back: it closes the keyboard first, so it never sends one Back too many.
   - `lock` locks and wakes the emulator.
7. **Cross-check** each step, and keep what you find for its panel:
   - the hub's access log in Loki, with times: `{container="household-hub-dev"} |= "HTTP/1.1"`;
   - the hub's own log lines for the feature;
   - the app's request and response bodies from `applog.txt`;
   - a read-only query on the dev hub's database (`mode=ro`).
   Decline every write card.
8. **Each finding: new or already there?** Install master's build on the same emulator (`git switch --detach origin/master`, install, switch back), repeat the trigger, then put the PR's build back. Compare with the boards on the canvas. File what's already on master as an issue when I ask.
9. **Review the videos.** `python sheets.py sheet <video dir> <out dir>`, read each sheet, then `sheets.py frames` around every transition: opening a chat, the lock, Send, the pad. A dump taken seconds later misses what flashes for half a second.
10. **Report** as a private artifact, one panel per scenario: Setup, each step, extras, Cleanup.
    - Write `out/<pr>-report/scenarios.json`; its shape is at the top of `report.mjs`. Each panel holds its video, what should show and what did, what you did, screens, hub and app log lines, database checks and its findings.
    - Blur personal data first, such as the calendar account line on Profile: `sheets.py blur`. Shrink the screens with `sheets.py shrink`.
    - `node report.mjs build <scenarios.json> <outdir>`, then publish `<outdir>/index.html` with the files map it prints. Send the videos too.
11. **Leave things as found:**
    - the PR's build on the emulator, in light mode;
    - the test rows deleted. Remove a memory with the app's ✕: a row deleted by hand leaves its search vector behind;
    - `emu.sh logstop`, and no `adb logcat` left in `ps -ef`;
    - the app repo back on its branch, and the emulator running.
    Present a plan before coding any fix.
