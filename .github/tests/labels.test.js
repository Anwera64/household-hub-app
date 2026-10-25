const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const ROOT_DIR = path.resolve(__dirname, '..', '..');
const LABELS_PATH = path.join(ROOT_DIR, '.github/workflows/labels.yml');

// Missing files read as empty so each check fails with its own message. CRLF checkouts are normalised.
const read = (file) => (fs.existsSync(file) ? fs.readFileSync(file, 'utf8').replace(/\r\n/g, '\n') : '');

const topLevelBlock = (content, key) => {
  const match = content.match(new RegExp(`^${key}:[^\\n]*\\n((?:[ \\t]+[^\\n]*\\n|[ \\t]*\\n)*)`, 'm'));
  return match ? match[1] : '';
};

test('Pull request labels workflow', async (t) => {
  const workflow = read(LABELS_PATH);

  await t.test('runs on pull requests, again whenever their labels change', () => {
    assert.ok(fs.existsSync(LABELS_PATH), '.github/workflows/labels.yml must exist');
    const on = topLevelBlock(workflow, 'on');
    assert.match(on, /^  pull_request:\n/m);
    assert.match(on, /types: \[opened, reopened, synchronize, labeled, unlabeled\]/);
    assert.doesNotMatch(workflow, /pull_request_target/, 'fork code never gets a write token here');
  });

  await t.test('only reads pull requests', () => {
    assert.equal(topLevelBlock(workflow, 'permissions').trim(), 'pull-requests: read');
  });

  await t.test('fails unless the pull request carries exactly one of the four labels, read with jq', () => {
    assert.ok(workflow.includes('${{ toJSON(github.event.pull_request.labels.*.name) }}'), 'labels come from the event payload');
    assert.match(workflow, /\bjq\b/, 'parsed with jq');
    for (const label of ['feature', 'fix', 'dependencies', 'chore']) {
      assert.ok(workflow.includes(label), `${label} must be one of the accepted labels`);
    }
    assert.match(workflow, /exit 1/, 'a wrong count fails the job');
    assert.match(workflow, /::error::[^\n]*feature[^\n]*fix[^\n]*dependencies[^\n]*chore/, 'the failure names the four labels');
  });

  await t.test('uses no third-party action', () => {
    assert.doesNotMatch(workflow, /^\s*(?:-\s*)?uses:/m, 'jq and the event payload are enough');
  });
});
