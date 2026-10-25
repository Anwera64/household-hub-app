const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const ROOT_DIR = path.resolve(__dirname, '..', '..');
const RENOVATE_PATH = path.join(ROOT_DIR, 'renovate.json');

// A missing or invalid file reads as an empty config so each check fails with its own message.
function readConfig() {
  try {
    return JSON.parse(fs.readFileSync(RENOVATE_PATH, 'utf8'));
  } catch {
    return {};
  }
}

test('Renovate configuration', async (t) => {
  const config = readConfig();
  const rules = config.packageRules ?? [];

  await t.test('renovate.json exists and is valid JSON', () => {
    assert.ok(fs.existsSync(RENOVATE_PATH), 'renovate.json must exist');
    assert.ok(Object.keys(config).length > 0, 'renovate.json must parse');
    assert.equal(config.$schema, 'https://docs.renovatebot.com/renovate-schema.json');
    assert.deepEqual(config.extends, ['config:recommended']);
  });

  await t.test('watches only Gradle and the Gradle wrapper', () => {
    assert.deepEqual(config.enabledManagers, ['gradle', 'gradle-wrapper']);
  });

  await t.test('waits three days after a release and merges through pull requests', () => {
    assert.equal(config.timezone, 'Europe/Madrid');
    assert.equal(config.minimumReleaseAge, '3 days');
    assert.equal(config.minimumReleaseAgeBehaviour, 'timestamp-optional');
    assert.equal(config.semanticCommits, 'disabled');
    assert.equal(config.automergeType, 'pr');
    assert.equal(config.platformAutomerge, false);
  });

  await t.test('labels every pull request dependencies, so the release notes and the label check agree', () => {
    assert.deepEqual(config.labels, ['dependencies']);
  });

  await t.test('does not run lock file maintenance', () => {
    assert.ok(!config.lockFileMaintenance || config.lockFileMaintenance.enabled === false, 'lockFileMaintenance must be off');
  });

  await t.test('groups minor and patch bumps into one automerged pull request', () => {
    const rule = rules.find((r) => r.groupName === 'app dependencies');
    assert.ok(rule, 'a rule must group "app dependencies"');
    assert.deepEqual(rule.matchManagers, ['gradle', 'gradle-wrapper']);
    assert.deepEqual(rule.matchUpdateTypes, ['minor', 'patch']);
    assert.equal(rule.automerge, true);
    assert.equal(rule.matchFileNames, undefined, 'the whole repository is the app: no file filter');
  });

  await t.test('moves Kotlin, Compose and AGP together, after the group above so it overrides it', () => {
    const rule = rules.find((r) => r.groupName === 'Kotlin, Compose and AGP');
    assert.ok(rule, 'a rule must group "Kotlin, Compose and AGP"');
    assert.deepEqual(rule.matchPackageNames, ['/^org\\.jetbrains\\.kotlin[.:]/', '/^org\\.jetbrains\\.compose[.:]/', '/^com\\.android\\./']);
    assert.ok(rules.indexOf(rule) > rules.findIndex((r) => r.groupName === 'app dependencies'), 'it must come after the minor/patch group');
  });

  await t.test('has no rule left over from the monorepo', () => {
    const text = JSON.stringify(config);
    assert.ok(!text.includes('apps/household-hub'), 'no monorepo path');
    for (const word of ['docker', 'pip-compile', 'bump-hub-patch', 'container images']) {
      assert.ok(!text.includes(word), `${word} belongs to the homelab repository`);
    }
  });
});
