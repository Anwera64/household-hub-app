const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const ROOT_DIR = path.resolve(__dirname, '..', '..');
const SCRIPT = '.github/scripts/release-facts.sh';

// The tags as release-tags.yml pipes them in: `git ls-remote --tags origin 'refs/tags/v*'` lines.
const lsRemote = (tags) => tags.map((tag, i) => `${String(i).padStart(40, '0')}\trefs/tags/${tag}\n`).join('');

// Runs the script for a new tag, with the repository's tags on stdin, and reads its key=value lines.
const facts = (tag, tags) => {
  const run = spawnSync('bash', [SCRIPT, tag], { cwd: ROOT_DIR, input: lsRemote(tags), encoding: 'utf8' });
  assert.equal(run.status, 0, `${SCRIPT} ${tag} must exit 0 (stderr: ${run.stderr})`);
  return Object.fromEntries(run.stdout.trim().split('\n').map((line) => line.split(/=(.*)/s).slice(0, 2)));
};

test('A release is the latest when it is the highest version, and its notes start at the release before it', async (t) => {
  await t.test('GIVEN 0.5.5 is the last patch WHEN 0.6.0 is released from its release branch THEN it is the latest and its notes start at v0.5.5', () => {
    const tags = ['v0.5.0', 'v0.5.4', 'v0.5.5', 'v0.6.0'];
    assert.deepEqual(facts('v0.6.0', tags), { latest: 'true', notes_start: 'v0.5.5' });
  });

  await t.test('GIVEN 0.7.0 is out WHEN 0.6.1 is released THEN it is not the latest and its notes start at v0.6.0', () => {
    const tags = ['v0.5.5', 'v0.6.0', 'v0.7.0', 'v0.6.1'];
    assert.deepEqual(facts('v0.6.1', tags), { latest: 'false', notes_start: 'v0.6.0' });
  });

  await t.test('GIVEN 0.6 is the newest minor WHEN its patch 0.6.1 is released THEN it is the latest', () => {
    const tags = ['v0.5.5', 'v0.6.0', 'v0.6.1'];
    assert.deepEqual(facts('v0.6.1', tags), { latest: 'true', notes_start: 'v0.6.0' });
  });

  await t.test('GIVEN 0.9.1 is out WHEN 0.10.0 is released THEN versions compare by number and 0.10.0 is the latest', () => {
    const tags = ['v0.1.0', 'v0.9.0', 'v0.9.1', 'v0.10.0'];
    assert.deepEqual(facts('v0.10.0', tags), { latest: 'true', notes_start: 'v0.9.1' });
  });

  await t.test('GIVEN tags that are not versions WHEN 0.6.0 is released THEN they are ignored', () => {
    const tags = ['v0.5.5', 'v0.5.9-beta', 'v0.7.0-rc1', 'v1.0', 'vnext', 'v0.5.5^{}', 'v0.6.0'];
    assert.deepEqual(facts('v0.6.0', tags), { latest: 'true', notes_start: 'v0.5.5' });
  });

  await t.test('GIVEN no release yet WHEN the first version is released THEN it is the latest and its notes have no start', () => {
    assert.deepEqual(facts('v0.1.0', ['v0.1.0']), { latest: 'true', notes_start: '' });
  });
});
