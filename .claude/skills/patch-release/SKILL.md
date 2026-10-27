---
name: patch-release
description: Ship a fix from master as a patch release on the latest minor's release branch
---
Patches are only for the latest minor. Given the fix's merged commit on master (or its PR number) and the minor to patch (none given: the minor of the latest release tag):
1. Check the fix is already on master. Never cherry-pick something that isn't: fix it on master first.
2. `git fetch --tags`. If `release/x.y` does not exist yet, it will be cut from tag `vX.Y.0`.
3. Look at the existing `vX.Y.*` tags and pick the next free patch version.
4. Present the plan to me: which branch, which commit, which version. Wait for my approval before pushing anything.
5. After I approve: create `release/x.y` from `vX.Y.0` if needed and push it (it is protected, so no direct commits to it afterwards). Create `fix/<name>-x.y` from `release/x.y`.
6. Cherry-pick the fix with `-x`. Resolve conflicts and explain each resolution.
7. Bump the version in both places: `app.version` in `gradle.properties`, and the version and build number in `iosApp/project.yml` (1.2.3 becomes 10203). `AppVariantsTest` fails when they differ.
8. Run the checks `./gradlew ktlintCheck jvmTest` with JAVA_HOME set as in the Setup wiki page. Stop and report if anything fails.
9. Push the branch and open a PR against `release/x.y` with the label `fix`. The description names the master PR it comes from.
10. After I merge it, check that the release appeared: tag `v<version>`, the GitHub release, and the Firebase and TestFlight uploads.
