---
name: test-pr
description: Rebase a PR and prepare it for manual testing with the dev app on the phone
---
Given a PR number (none given: the PR of the current branch):
1. Fetch latest master and rebase the PR branch on it. Resolve conflicts and explain each resolution. Push the result.
2. Run `./gradlew ktlintCheck jvmTest` with JAVA_HOME set as in the Setup wiki page. Stop and report if anything fails.
3. If the PR depends on hub changes that aren't released yet, the backend MR must be on the dev hub first (the backend repo's test-mr skill). Say so and stop until it is.
4. Install the dev app on the connected phone: `.\gradlew.bat :androidApp:installDebug`, then `adb shell am start -n com.homelab.household.dev/com.homelab.household.MainActivity`. The dev app talks to the dev hub (`hub.devUrl` in `gradle.properties`).
5. Give me ONE unified manual test prompt covering the PR's changes.
6. While I test, investigate what I report. The app's logs reach the household's log store; ask me before guessing a cause.
7. Present a plan before coding any fix. After I confirm it works, push and check CI.
