const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const ROOT_DIR = path.resolve(__dirname, '..', '..');
const RELEASE_PATH = path.join(ROOT_DIR, '.github/workflows/release.yml');
const RELEASE_TAGS_PATH = path.join(ROOT_DIR, '.github/workflows/release-tags.yml');

// Missing files read as empty so each check fails with its own message. CRLF checkouts are normalised.
const read = (file) => (fs.existsSync(file) ? fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n') : '');

// Text of a top-level YAML key's block, up to the next top-level key.
const topLevelBlock = (content, key) => {
  const match = content.match(new RegExp(`^${key}:[^\\n]*\\n((?:[ \\t]+[^\\n]*\\n|[ \\t]*\\n)*)`, 'm'));
  return match ? match[1] : '';
};

const usesRefs = (workflow) => [...workflow.matchAll(/^\s*(?:-\s*)?uses:\s*["']?([^\s"'#]+)/gm)].map((m) => m[1]);

// Text of a job, up to the next job.
const jobBlock = (workflow, name) => {
  const match = workflow.match(new RegExp(`^  ${name}:\\n((?:    [^\\n]*\\n|[ \\t]*\\n)*)`, 'm'));
  return match ? match[1] : '';
};

test('App release workflow', async (t) => {
  const workflow = read(RELEASE_PATH);
  const job = (name) => jobBlock(workflow, name);

  await t.test('runs only when the tagging workflow calls it, never on a pushed tag', () => {
    assert.ok(fs.existsSync(RELEASE_PATH), '.github/workflows/release.yml must exist');
    const on = topLevelBlock(workflow, 'on');
    // A tag anyone can push would release whatever commit it points at; a merge has been reviewed.
    const triggers = [...on.matchAll(/^  (\w+):/gm)].map((m) => m[1]);
    assert.deepEqual(triggers, ['workflow_call'], 'release-tags.yml is the only way in');
    assert.match(on, /^  workflow_call:\n    inputs:\n      tag:\n/m);
    assert.match(on, /^      tag:\n(?:        [^\n]*\n)*?        required: true\n(?:        [^\n]*\n)*?        type: string\n/m, 'tag is a required string');
    assert.match(on, /^      latest:\n(?:        [^\n]*\n)*?        type: string\n/m, 'latest is a string input');
    assert.match(on, /^      latest:\n(?:        [^\n]*\n)*?        default: "true"\n/m, 'latest defaults to "true"');
    assert.doesNotMatch(on, /pull_request/, 'no pull request reaches the signing keys');
    assert.deepEqual(topLevelBlock(workflow, 'permissions').trim().split('\n').map((l) => l.trim()), ['contents: write']);
  });

  await t.test('has no monorepo leftovers: no app-v tags, no apps/ paths', () => {
    assert.ok(!workflow.includes('app-v'), 'the tags are plain v<version> now');
    assert.ok(!workflow.includes('apps/household-hub'), 'the app is at the repository root');
    assert.doesNotMatch(workflow, /working-directory: \.\s*$/m, 'there is no subdirectory to step out of');
  });

  await t.test('refuses a tag that is not app.version, and tests before anything is built for release', () => {
    const check = job('check');
    assert.ok(check, 'a check job');
    assert.match(check, /sed -n 's\/\^app\\\.version=\/\/p' gradle\.properties/);
    assert.match(check, /\[ "v\$version" = "\$TAG" \]/);
    assert.match(check, /\.\/gradlew jvmTest/);
    for (const name of ['android', 'ios']) {
      assert.match(job(name), /needs: check/, `${name} waits for the check`);
    }
  });

  await t.test('only the upload jobs see the keys, through the release environment', () => {
    assert.doesNotMatch(job('check'), /secrets\./, 'the check job needs no secret');
    for (const name of ['android', 'ios']) {
      assert.match(job(name), /^    environment: release$/m, `${name} runs in the release environment`);
    }
  });

  await t.test('Android: a signed release build to Firebase App Distribution, nothing kept', () => {
    const android = job('android');
    assert.match(android, /\.\/gradlew :androidApp:assembleRelease/);
    for (const name of ['HUB_KEYSTORE_PASSWORD', 'HUB_KEY_ALIAS', 'HUB_KEY_PASSWORD']) {
      assert.ok(android.includes(`${name}: \${{ secrets.${name} }}`), `${name} comes from the release environment`);
    }
    assert.match(android, /base64 --decode > "\$RUNNER_TEMP\/release\.keystore"/, 'the keystore lives outside the checkout');
    assert.match(android, /npx --yes firebase-tools@\d+\.\d+\.\d+ appdistribution:distribute /, 'a pinned Firebase CLI');
    assert.match(android, /androidApp\/build\/outputs\/apk\/release\/androidApp-release\.apk/);
    assert.match(android, /--app "\$\{\{ secrets\.FIREBASE_ANDROID_APP_ID \}\}"/);
    assert.match(android, /--groups household/);
    // No key to leak: GitHub vouches for the run, and Google lets only this repo's release runs act
    // as the account (Workload Identity Federation). The action exports GOOGLE_APPLICATION_CREDENTIALS.
    assert.match(android, /uses: google-github-actions\/auth@[0-9a-f]{40} # v3\.\d+\.\d+/);
    assert.match(android, /workload_identity_provider: \$\{\{ secrets\.FIREBASE_WIF_PROVIDER \}\}/);
    assert.match(android, /service_account: \$\{\{ secrets\.FIREBASE_SERVICE_ACCOUNT_EMAIL \}\}/);
    assert.match(android, /^    permissions:\n      contents: read\n      id-token: write\n/m);
    assert.doesNotMatch(workflow, /FIREBASE_SERVICE_ACCOUNT\b(?!_EMAIL)|firebase\.json/, 'no service account key');
  });

  await t.test('iOS: an archive signed with the App Store Connect key, uploaded to TestFlight', () => {
    const ios = job('ios');
    assert.match(ios, /runs-on: macos-26/, 'the image the CI builds iOS on');
    assert.match(ios, /working-directory: iosApp\n/, 'the Xcode project is generated in iosApp');
    assert.match(ios, /xcodegen generate/);
    assert.match(ios, /xcodebuild archive[\s\S]*?-configuration Release/);
    assert.match(ios, /xcodebuild -exportArchive[\s\S]*?-exportOptionsPlist ExportOptions\.plist/);
    for (const flag of ['-allowProvisioningUpdates', '-authenticationKeyPath', '-authenticationKeyID', '-authenticationKeyIssuerID']) {
      assert.ok(ios.split(flag).length - 1 >= 2, `archive and export both pass ${flag}`);
    }
    assert.match(ios, /DEVELOPMENT_TEAM="\$\{\{ secrets\.APPLE_TEAM_ID \}\}"/);
    assert.match(ios, /MARKETING_VERSION="\$version" CURRENT_PROJECT_VERSION="\$code"/, 'built as the tagged version');
    const options = read(path.join(ROOT_DIR, 'iosApp/ExportOptions.plist'));
    assert.match(options, /<key>method<\/key>\s*<string>app-store-connect<\/string>/);
    assert.match(options, /<key>destination<\/key>\s*<string>upload<\/string>/);
    assert.match(options, /<key>signingStyle<\/key>\s*<string>automatic<\/string>/);
  });

  await t.test('writes release notes only once both stores have the build, and keeps no build', () => {
    const notes = job('notes');
    assert.match(notes, /needs: \[android, ios\]/);
    assert.match(notes, /^\s*gh release create "\$TAG" --verify-tag --generate-notes --title "\$TAG" /m);
    assert.doesNotMatch(workflow, /upload-artifact|gh release upload/, 'no APK or IPA leaves the runners');
  });

  await t.test('marks the release latest and starts its notes where the caller says', () => {
    const on = topLevelBlock(workflow, 'on');
    assert.match(on, /^      notes_start:\n(?:        [^\n]*\n)*?        type: string\n/m, 'notes_start is a string input');
    assert.match(on, /^      notes_start:\n(?:        [^\n]*\n)*?        default: ""\n/m, 'notes_start defaults to ""');
    assert.doesNotMatch(on, /^      notes_start:\n(?:        [^\n]*\n)*?        required: true\n/m, 'notes_start is optional');
    const notes = job('notes');
    assert.ok(notes.includes("--latest=${{ inputs.latest || 'true' }}"), "the release is created with --latest=${{ inputs.latest || 'true' }}");
    assert.ok(notes.includes('NOTES_START: ${{ inputs.notes_start }}'), 'the step reads notes_start from its env');
  });

  await t.test('passes --notes-start-tag only when the caller names a start', () => {
    // The step's script, run with gh stubbed to print its arguments one per line.
    const script = job('notes').match(/^        run: \|\n((?:          [^\n]*\n|[ \t]*\n)*)/m);
    assert.ok(script, 'the notes step runs a script');
    const run = script[1].replace(/^ {10}/gm, '').replace("${{ inputs.latest || 'true' }}", 'true');
    const ghArgs = (notesStart) => {
      const result = spawnSync('bash', ['-c', `gh() { printf '%s\\n' "$@"; }\n${run}`], {
        env: { ...process.env, TAG: 'v0.6.0', NOTES_START: notesStart },
        encoding: 'utf8',
      });
      assert.equal(result.status, 0, `the notes script must exit 0 (stderr: ${result.stderr})`);
      return result.stdout.trim().split('\n');
    };
    const started = ghArgs('v0.5.5');
    const flag = started.indexOf('--notes-start-tag');
    assert.ok(flag >= 0, 'a named start is passed as --notes-start-tag');
    assert.equal(started[flag + 1], 'v0.5.5', '--notes-start-tag is followed by the start tag');
    assert.ok(!ghArgs('').includes('--notes-start-tag'), 'no start, no --notes-start-tag: the first release');
  });

  await t.test('pins every action to a full commit SHA', () => {
    for (const ref of usesRefs(workflow).filter((r) => !r.startsWith('./'))) {
      assert.match(ref, /@[0-9a-f]{40}$/, `"${ref}" must be pinned to a 40-character commit SHA`);
    }
  });
});

test('Merging an app version bump releases it', async (t) => {
  const workflow = read(RELEASE_TAGS_PATH);

  await t.test('runs on master and release branches, only when the version or this workflow changed', () => {
    assert.ok(fs.existsSync(RELEASE_TAGS_PATH), '.github/workflows/release-tags.yml must exist');
    const on = topLevelBlock(workflow, 'on');
    const push = on.match(/^  push:\n((?:    [^\n]*\n)*)/m);
    assert.ok(push, 'must run on push');
    assert.match(push[1], /branches: \[master, 'release\/\*\*'\]/);
    const paths = [...push[1].matchAll(/^      - (\S+)$/gm)].map((m) => m[1]).sort();
    assert.deepEqual(paths, ['.github/workflows/release-tags.yml', 'gradle.properties']);
    assert.doesNotMatch(on, /pull_request|tags:/, 'only a merge tags');
    assert.match(topLevelBlock(workflow, 'permissions'), /^  contents: read$/m, 'jobs ask for more themselves');
  });

  await t.test('has no hub leftovers or monorepo paths', () => {
    assert.ok(!workflow.includes('hub-v'), 'the hub is released from its own repository');
    assert.ok(!workflow.includes('app-v'), 'the tags are plain v<version> now');
    assert.ok(!workflow.includes('apps/household-hub'), 'the app is at the repository root');
  });

  await t.test('tags app.version once, and never moves a tag', () => {
    const tag = jobBlock(workflow, 'tag');
    assert.ok(tag, 'a tag job');
    assert.match(tag, /permissions:\n      contents: write\n/);
    assert.match(tag, /sed -n 's\/\^app\\\.version=\/\/p' gradle\.properties/);
    assert.match(tag, /tag="v\$version"/);
    assert.match(tag, /git ls-remote --exit-code --tags origin "refs\/tags\/\$tag"/, 'an existing tag is never moved');
    assert.match(tag, /git tag "\$tag" "\$GITHUB_SHA"/);
    assert.match(tag, /git push origin "refs\/tags\/\$tag"/);
    assert.match(tag, /echo "app=\$tag" >> "\$GITHUB_OUTPUT"/, 'only a new tag is handed on');
    assert.match(tag, /outputs:\n      app: \$\{\{ steps\.app\.outputs\.app \}\}/);
  });

  await t.test('counts a version as released only when its release exists, and stops loudly on a tag without one', () => {
    const tag = jobBlock(workflow, 'tag');
    assert.match(tag, /^        env:\n          GH_TOKEN: \$\{\{ github\.token \}\}\n/m, 'gh signs in with the job token');
    const released = tag.match(/^ +if gh release view "\$tag"[^\n]*; then\n((?: +[^\n]*\n)*?) +fi\n/m);
    assert.ok(released, 'gh release view, not the tag, says whether the version is released');
    assert.match(released[1], /exit 0/, 'a released version does nothing');
    // A tag pushed by hand, or left by a release run that broke: releasing nothing quietly would hide it.
    const tagged = tag.match(/^ +if git ls-remote --exit-code --tags origin "refs\/tags\/\$tag"[^\n]*; then\n((?: +[^\n]*\n)*?) +fi\n/m);
    assert.ok(tagged, 'an unreleased version still looks for its tag');
    const error = tagged[1].match(/echo "::error::([^\n]*)"\n/);
    assert.ok(error, 'a tag without a release is an error on the run');
    for (const hint of [/no release/, /pushed by hand/, /release run failed/, /commit/, /delete the tag/, /re-run/]) {
      assert.match(error[1], hint, 'the error says what happened and what to do');
    }
    assert.match(tagged[1], /exit 1/);
    assert.doesNotMatch(tagged[1], /exit 0/, 'a tag without a release is never skipped');
    assert.equal(tag.match(/exit 0/g)?.length, 1, 'only an existing release skips');
    const order = ['gh release view', 'git ls-remote', 'git tag "$tag"'].map((step) => tag.indexOf(step));
    assert.deepEqual([...order].sort((a, b) => a - b), order, 'the release is asked for, then the tag, then the version is tagged');
  });

  await t.test('works out from the version tags whether the release is the latest and where its notes start', () => {
    const tag = jobBlock(workflow, 'tag');
    assert.ok(tag.includes('latest: ${{ steps.facts.outputs.latest }}'), 'the tag job exposes latest');
    assert.ok(tag.includes('notes_start: ${{ steps.facts.outputs.notes_start }}'), 'the tag job exposes notes_start');
    const facts = tag.match(/^      - name: [^\n]*\n        id: facts\n((?:        [^\n]*\n)*)/m);
    assert.ok(facts, 'a step with id facts');
    assert.match(facts[1], /^        if: steps\.app\.outputs\.app != ''$/m, 'only for a version this run tagged');
    assert.match(facts[1], /^          TAG: \$\{\{ steps\.app\.outputs\.app \}\}$/m, 'the new tag comes in through env');
    assert.ok(
      facts[1].includes(`git ls-remote --tags origin 'refs/tags/v*' | bash .github/scripts/release-facts.sh "$TAG" >> "$GITHUB_OUTPUT"`),
      'the version tags are piped into release-facts.sh, which writes the outputs',
    );
    assert.match(facts[1], /set -o pipefail/, 'a failed ls-remote stops the run instead of reading as no tags');
    assert.ok(tag.indexOf('release-facts.sh') > tag.indexOf('git push origin'), 'the facts are read after the new tag is pushed');
  });

  await t.test('calls the release with the keys of the release environment, and the facts of the tag job', () => {
    const app = jobBlock(workflow, 'app');
    assert.match(app, /needs: tag/);
    assert.match(app, /if: needs\.tag\.outputs\.app != ''/);
    assert.match(app, /uses: \.\/\.github\/workflows\/release\.yml/);
    assert.match(app, /tag: \$\{\{ needs\.tag\.outputs\.app \}\}/);
    assert.ok(app.includes('latest: ${{ needs.tag.outputs.latest }}'), 'latest comes from the tags, not the branch');
    assert.ok(app.includes('notes_start: ${{ needs.tag.outputs.notes_start }}'), 'the notes start where the tag job says');
    assert.doesNotMatch(workflow, /github\.ref_name == 'master'/, 'the branch no longer decides latest');
    assert.match(app, /secrets: inherit/);
    // A called workflow gets no more than its caller grants: the Firebase sign-in needs id-token.
    assert.match(app, /permissions:\n      contents: write\n      id-token: write\n/);
  });

  await t.test('pins every action to a full commit SHA', () => {
    const refs = usesRefs(workflow).filter((ref) => !ref.startsWith('./'));
    assert.ok(refs.length >= 1, 'checkout');
    for (const ref of refs) {
      assert.match(ref, /@[0-9a-f]{40}$/, `"${ref}" must be pinned to a 40-character commit SHA`);
    }
  });
});

test("Only the owner's review counts", async (t) => {
  const codeowners = read(path.join(ROOT_DIR, '.github/CODEOWNERS'));

  await t.test('owns every file, with no later rule handing a path to anyone else', () => {
    assert.ok(codeowners, '.github/CODEOWNERS must exist');
    const rules = codeowners.split('\n').map((line) => line.trim()).filter((line) => line && !line.startsWith('#'));
    assert.deepEqual(rules, ['* @Anwera64']);
  });
});

test('Release notes are grouped by label', async (t) => {
  const config = read(path.join(ROOT_DIR, '.github/release.yml'));

  await t.test('lists Features, Fixes, Dependencies and everything else, in that order', () => {
    assert.ok(config, '.github/release.yml must exist');
    const categories = [...config.matchAll(/^    - title: (.+)\n      labels:\n        - (.+)$/gm)].map((m) => [m[1].trim(), m[2].trim()]);
    assert.deepEqual(categories.map(([title, label]) => [title.replace(/["']/g, ''), label.replace(/["']/g, '')]), [
      ['Features', 'feature'],
      ['Fixes', 'fix'],
      ['Dependencies', 'dependencies'],
      ['Other changes', '*'],
    ]);
  });

  await t.test('leaves out a pull request whose fix already shipped in a patch, everywhere in the notes', () => {
    const items = (block) => [...block.matchAll(/^ *- (.+)$/gm)].map((m) => m[1].trim().replace(/["']/g, ''));
    const changelog = topLevelBlock(config, 'changelog');
    const excluded = changelog.match(/^  exclude:\n    labels:\n((?:      - [^\n]*\n)*)/m);
    assert.ok(excluded, 'changelog.exclude.labels');
    assert.deepEqual(items(excluded[1]), ['shipped-in-patch']);
    // GitHub doesn't document whether the global exclude outranks a category match, so every
    // category leaves it out too, and the pull request has nowhere to show.
    const categories = changelog.split(/^    - title: /m).slice(1);
    assert.equal(categories.length, 4, 'four categories');
    for (const category of categories) {
      const title = category.split('\n')[0].trim();
      const categoryExcluded = category.match(/^      exclude:\n        labels:\n((?:          - [^\n]*\n)*)/m);
      assert.ok(categoryExcluded, `${title} has exclude.labels`);
      assert.deepEqual(items(categoryExcluded[1]), ['shipped-in-patch'], `${title} leaves out shipped-in-patch`);
    }
  });
});
