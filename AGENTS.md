# Agent Guidelines & Workflow Rules

This is the Household Hub app (Kotlin Multiplatform client for Android and iOS).

## Where things live
- The hub backend and the homelab configuration are in other (private) repositories. Design docs and slice issues live in the homelab repository. The hub's API contract is read from the private repository `household-hub-contract`.

## Plan first
- Investigate read-only, then present a plan through plan mode (problem, architecture, file-by-file changes, open questions, verification) and wait for explicit approval before writing, deleting, or running anything that modifies state.
- This covers every change, including one-line fixes and follow-ups in the same session. Don't write plan files into the repository.

## Test-driven development
- Work in red-green-refactor cycles: write the test and watch it fail before any implementation code, then the minimal code to pass, then refactor with the full suite green.
- Each plan lists its cycles in order, with the Red tests and their expected failures, the Green change, and the Refactor step.
- Test conventions (names, robots) are in "Tests" below.

## Clean architecture
Dependencies point inward: `presentation -> domain <- data`.
- **domain** is the core: entities, use cases, repository interfaces and domain exceptions. It depends on no framework, database or other layer.
- **presentation** depends only on domain: UI, their models, and mappers to domain.
- **data** depends only on domain: it implements the repository interfaces, with data sources, storage models and mappers to domain.
- Models never cross a layer boundary; each boundary has its own mappers, so a model change doesn't leak.
- Wiring happens only in DI code, and only the app's root assembles the graph.
- Boundary tests enforce these rules on every test run and in the pre-commit hook. A new rule gets a test.
- **Client (Kotlin Multiplatform):** Gradle modules `:core:domain`, `:core:data` and `:core:presentation`, each with its own Koin module, assembled by the app. Boundaries are tested in `shared/src/jvmTest/.../architecture/`.

## Sub-Agent Rule: Keep Token Cost Down
- **Delegate what doesn't need the main model's judgement:** **Sonnet** for simple, well-specified tasks, **Haiku** for the most basic ones (lookups, boilerplate, doc checkboxes).
- **The main agent keeps design decisions** and reviews every delegated diff before it is committed.
- **Hand-offs are complete:** a sub-agent starts cold, so give it the tests, specs and file paths it needs.

## Builds
- Build and test client (Kotlin) code before committing; if the environment can't build it, say so clearly in the commit and PR.

## Branches and releases
- Trunk-based: `master` is the trunk. New work branches from `master` as `feat/...`, `fix/...` or `chore/...`, and reaches it through a pull request.
- Every PR carries exactly one of the labels `feature`, `fix`, `dependencies` or `chore` (`labels.yml` checks it, and the release notes are grouped by it).
- Merging a bump of `app.version` in `gradle.properties` releases it, tagged `v<version>`.
- Patches for the latest minor: fix on `master` first, then cherry-pick into `release/x.y`, cut from the tag `vX.Y.0` when first needed. No new release branches for older minors.

## Tests
- Name every test `` `GIVEN <situation> WHEN <action> THEN <outcome>` ``, in household language rather than class or method names. The name carries the structure: the body has no `// GIVEN`, `// WHEN` or `// THEN` comments.
- No commas in a backticked name: Kotlin/Native rejects them, and `commonTest` compiles for iOS too, so a green `jvmTest` proves nothing. `TestNameCompatibilityTest` catches it.
- Android instrumented tests (`androidTest`, `androidDeviceTest`) use underscores, `GIVEN_x_WHEN_y_THEN_z`: a space isn't legal dex below API 30, and minSdk is 26.
- Unit and ViewModel tests do one action on the subject. A test that needs two actions is two tests.
- Screen and component tests go through a robot: one `<Screen>Robot` per screen or component, reached with `on<Screen> { … }` inside `runScreenTest`, with only the hub faked. Robot methods are what a person does or sees (`removes`, `seesMemory`, `cannotRemove`), and on-screen text comes from the string resources, never from copied literals. A robot script may chain actions and checks.
