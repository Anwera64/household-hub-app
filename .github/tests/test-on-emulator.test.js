const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawn, spawnSync } = require('node:child_process');

const ROOT_DIR = path.resolve(__dirname, '..', '..');
const SCRIPTS_DIR = path.join(ROOT_DIR, '.claude/skills/test-on-emulator/scripts');
const EMU = path.join(SCRIPTS_DIR, 'emu.sh');
const REPORT = path.join(SCRIPTS_DIR, 'report.mjs');

// emu.sh drives adb through bash. On Windows these tests skip; run them there in node:22 (Docker).
const HAS_BASH = process.platform !== 'win32' && spawnSync('bash', ['--version']).status === 0;

// A stand-in for adb: it logs every call, serves a canned UI dump, a fake screenshot and an app
// pid, and a logcat that prints one request line every 0.1 s until it is stopped.
const FAKE_ADB = `#!/usr/bin/env bash
echo "$*" >> "$FAKE_ADB_LOG"
if [ "$1" = logcat ]; then
  while true; do echo "11-19 18:00:00.000 D/HttpClient(4242): REQUEST: https://hub/api/v1/memories/page"; sleep 0.1; done
fi
case "$1 $2" in
  "exec-out cat") cat "$FAKE_ADB_DUMP" ;;
  "exec-out screencap") printf 'PNG' ;;
  "shell pidof") echo 4242 ;;
esac
exit 0
`;

// A throwaway world for one test: the fake adb first on PATH, emu.sh's state and output in a temp dir.
function sandbox(nodes = []) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'emu-'));
  const bin = path.join(dir, 'bin');
  fs.mkdirSync(bin);
  fs.writeFileSync(path.join(bin, 'adb'), FAKE_ADB, { mode: 0o755 });
  const dump = path.join(dir, 'dump.xml');
  const xml = nodes
    .map((n) => `<node text="${n.text || ''}" content-desc="${n.desc || ''}" bounds="${n.bounds}"/>`)
    .join('');
  fs.writeFileSync(dump, `<hierarchy>${xml}</hierarchy>`);
  const env = {
    ...process.env,
    PATH: `${bin}${path.delimiter}${process.env.PATH}`,
    TMPDIR: dir,
    EMU_OUT: path.join(dir, 'out'),
    FAKE_ADB_LOG: path.join(dir, 'adb.log'),
    FAKE_ADB_DUMP: dump,
  };
  delete env.HUB_DEV_PIN;
  delete env.ANDROID_SERIAL;
  const adbLog = () => (fs.existsSync(env.FAKE_ADB_LOG) ? fs.readFileSync(env.FAKE_ADB_LOG, 'utf8') : '');
  return { dir, env, adbLog };
}

function emu(sb, args, extraEnv = {}) {
  return spawnSync('bash', [EMU, ...args], { env: { ...sb.env, ...extraEnv }, encoding: 'utf8', timeout: 20000 });
}

// A long-running emu.sh command (logwatch), in its own process group so a test can always clean up.
function emuBackground(sb, args) {
  const child = spawn('bash', [EMU, ...args], { env: sb.env, detached: true });
  child.out = '';
  child.stdout.on('data', (d) => { child.out += d; });
  child.exited = new Promise((resolve) => child.on('exit', resolve));
  return child;
}

function killGroup(child) {
  try { process.kill(-child.pid, 'SIGKILL'); } catch { /* already gone */ }
}

const isAlive = (pid) => { try { process.kill(pid, 0); return true; } catch { return false; } };

async function waitUntil(check, ms = 4000) {
  const end = Date.now() + ms;
  while (Date.now() < end) {
    if (check()) return true;
    await new Promise((r) => setTimeout(r, 50));
  }
  return check();
}

const pidFile = (sb) => path.join(sb.dir, 'emu-sh-state', 'logwatch.pid');
const readPid = (sb) => (fs.existsSync(pidFile(sb)) ? Number(fs.readFileSync(pidFile(sb), 'utf8').trim()) : 0);

test('test-on-emulator: emu.sh on Git Bash', () => {
  // Git Bash's grep 3.0 aborts on every -i (exit 134, even on ASCII and with LC_ALL=C), and the
  // skill runs on Windows: a case-insensitive match has to go through awk's tolower instead.
  const script = fs.readFileSync(EMU, 'utf8');
  const caseless = script.split('\n').filter((l) => !l.trim().startsWith('#') && /\bgrep\s+(-\w*i\w*|--ignore-case)\b/.test(l));
  assert.deepEqual(caseless, [], 'emu.sh must not call grep -i');
});

test('test-on-emulator: emu.sh', { skip: !HAS_BASH && 'needs bash; on Windows run in node:22' }, async (t) => {
  await t.test('wait-for finds a label whatever its case on screen', () => {
    const sb = sandbox([{ text: 'NOT SAVED YET · 1', bounds: '[84,598][448,638]' }]);
    const r = emu(sb, ['wait-for', 'Not saved yet', '2']);
    assert.equal(r.status, 0, r.stdout + r.stderr);
  });

  // The PIN pad: each digit's key, with the centre a tap should hit.
  const PAD = [
    { text: '1', bounds: '[200,1400][300,1500]' }, { text: '2', bounds: '[600,1400][700,1500]' },
    { text: '5', bounds: '[600,1600][700,1700]' }, { text: '8', bounds: '[600,1800][700,1900]' },
    { text: '0', bounds: '[600,2000][700,2100]' }, { desc: '3 of 6 digits entered', bounds: '[0,700][1280,800]' },
  ];

  await t.test('pin taps the dev PIN from HUB_DEV_PIN on the pad, digit by digit', () => {
    const sb = sandbox(PAD);
    const r = emu(sb, ['pin'], { HUB_DEV_PIN: '2580' });
    assert.equal(r.status, 0, r.stdout + r.stderr);
    const taps = sb.adbLog().split('\n').filter((l) => l.startsWith('shell input tap'));
    assert.deepEqual(taps, ['shell input tap 650 1450', 'shell input tap 650 1650', 'shell input tap 650 1850', 'shell input tap 650 2050']);
  });

  await t.test('pin never prints the PIN', () => {
    const sb = sandbox(PAD);
    const r = emu(sb, ['pin'], { HUB_DEV_PIN: '2580' });
    assert.doesNotMatch(r.stdout + r.stderr, /2580/);
  });

  await t.test('pin without HUB_DEV_PIN says to ask for it and taps nothing', () => {
    const sb = sandbox(PAD);
    const r = emu(sb, ['pin']);
    assert.equal(r.status, 2);
    assert.match(r.stdout, /HUB_DEV_PIN is not set: ask for the dev PIN/);
    assert.doesNotMatch(sb.adbLog(), /input tap/);
  });

  await t.test('logwatch reads the app log with timestamps and keeps every line in applog.txt', async () => {
    const sb = sandbox();
    const watch = emuBackground(sb, ['logwatch']);
    try {
      const applog = path.join(sb.env.EMU_OUT, 'applog.txt');
      const read = () => (fs.existsSync(applog) ? fs.readFileSync(applog, 'utf8') : '');
      assert.ok(await waitUntil(() => read().split('\n').length > 3), 'applog.txt should fill');
      assert.match(sb.adbLog(), /^logcat -T 1 --pid=4242 -v time$/m);
      assert.match(read(), /^\d\d-\d\d \d\d:\d\d:\d\d\.\d{3} D\/HttpClient/m);
      assert.ok(await waitUntil(() => watch.out.includes('REQUEST:')), 'the filtered lines reach stdout');
    } finally {
      emu(sb, ['logstop']);
      killGroup(watch);
    }
  });

  await t.test('a second logwatch stops the first, so no line is written twice', async () => {
    const sb = sandbox();
    const first = emuBackground(sb, ['logwatch']);
    let second;
    try {
      assert.ok(await waitUntil(() => readPid(sb) > 0), 'the first watcher records its pid');
      const firstPid = readPid(sb);
      second = emuBackground(sb, ['logwatch']);
      assert.ok(await waitUntil(() => readPid(sb) > 0 && readPid(sb) !== firstPid), 'the second records its own');
      assert.ok(await waitUntil(() => !isAlive(firstPid)), 'the first adb logcat is stopped');
      const code = await Promise.race([first.exited, new Promise((r) => setTimeout(() => r('still running'), 4000))]);
      assert.notEqual(code, 'still running', 'the first logwatch ends');
    } finally {
      emu(sb, ['logstop']);
      killGroup(first);
      if (second) killGroup(second);
    }
  });

  await t.test('burst takes timed screenshots, named in order with their ms since the call', () => {
    const sb = sandbox();
    const r = emu(sb, ['burst', 'reload', '3']);
    assert.equal(r.status, 0, r.stdout + r.stderr);
    const shots = fs.readdirSync(sb.env.EMU_OUT).filter((f) => f.startsWith('reload-')).sort();
    assert.equal(shots.length, 3);
    shots.forEach((f, i) => assert.match(f, new RegExp(`^reload-0${i + 1}-\\d+ms\\.png$`)));
    const ms = shots.map((f) => Number(f.match(/-(\d+)ms\.png$/)[1]));
    assert.ok(ms[0] <= ms[1] && ms[1] <= ms[2], `ms should rise: ${ms}`);
    assert.equal(fs.readFileSync(path.join(sb.env.EMU_OUT, shots[0]), 'utf8'), 'PNG');
  });

  await t.test('stopping logwatch stops its adb logcat too', async () => {
    const sb = sandbox();
    const watch = emuBackground(sb, ['logwatch']);
    try {
      assert.ok(await waitUntil(() => readPid(sb) > 0), 'the watcher records its pid');
      const pid = readPid(sb);
      process.kill(watch.pid, 'SIGTERM');
      assert.ok(await waitUntil(() => !isAlive(pid)), 'adb logcat is stopped');
      assert.equal(fs.existsSync(pidFile(sb)), false);
    } finally {
      emu(sb, ['logstop']);
      killGroup(watch);
    }
  });

  await t.test('logstop stops the watcher and removes its pid file', async () => {
    const sb = sandbox();
    const watch = emuBackground(sb, ['logwatch']);
    try {
      assert.ok(await waitUntil(() => readPid(sb) > 0), 'the watcher records its pid');
      const pid = readPid(sb);
      const r = emu(sb, ['logstop']);
      assert.equal(r.status, 0, r.stdout + r.stderr);
      assert.ok(await waitUntil(() => !isAlive(pid)), 'adb logcat is stopped');
      assert.equal(fs.existsSync(pidFile(sb)), false);
      const code = await Promise.race([watch.exited, new Promise((r) => setTimeout(() => r('still running'), 4000))]);
      assert.notEqual(code, 'still running', 'logwatch ends');
    } finally {
      killGroup(watch);
    }
  });
});

// A small run: a setup panel without video, a step with a video and a screen, and a step whose
// words need escaping.
function reportFixture() {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'report-'));
  fs.mkdirSync(path.join(dir, 'media'));
  fs.writeFileSync(path.join(dir, 'media', 'step1.mp4'), 'video bytes');
  fs.writeFileSync(path.join(dir, 'media', 'shot1.jpg'), 'image bytes');
  const scenarios = {
    title: 'Held Facts Emulator Test',
    eyebrow: 'home/household-hub-app#39 · PR #49 · Android',
    intro: 'The manual test, run on the emulator.',
    meta: [{ label: 'Branch', value: 'feat/39' }],
    tiles: [{ n: '1 / 1', l: 'steps passed' }],
    findings: [{ kind: 'pre', label: 'Pre-existing', text: 'A row opens half hidden.', scenario: 's1' }],
    scenarios: [
      {
        id: 'setup', key: '0', name: 'Setup', title: 'Setup', badges: [{ kind: 'pre', label: 'Done' }],
        facts: [{ label: 'What I did', steps: ['Ran `emu.sh pin`.'] }],
      },
      {
        id: 's1', key: '1', name: 'Profile chip', title: '1 · Profile', badges: [{ kind: 'pass', label: 'Pass' }],
        video: { src: 'media/step1.mp4', caption: 'step1 · 8.8 s' },
        facts: [{ label: 'Showed', text: '“3 to check”' }],
        shots: [{ src: 'media/shot1.jpg', alt: 'Profile', caption: 'Profile' }],
        logs: [{ title: 'Hub', lines: [{ t: '18:43:56', text: 'GET /api/v1/memories/page 200' }, { t: '18:49:56', text: 'POST … 409 Conflict', bad: true }] }],
      },
      {
        id: 's2', key: '2', name: 'Escaping', title: 'Tap <Save> & "go"', badges: [{ kind: 'pass', label: 'Pass' }],
        facts: [{ label: 'Should show', items: ['a <b>bold</b> claim'] }],
      },
    ],
  };
  const json = path.join(dir, 'scenarios.json');
  fs.writeFileSync(json, JSON.stringify(scenarios));
  const out = path.join(dir, 'out');
  return { dir, json, out, scenarios };
}

function buildReport(json, out) {
  return spawnSync(process.execPath, [REPORT, 'build', json, out], { encoding: 'utf8', timeout: 20000 });
}

test('test-on-emulator: report.mjs', async (t) => {
  await t.test('builds one panel per scenario, in the order given, the first one showing', () => {
    const f = reportFixture();
    const r = buildReport(f.json, f.out);
    assert.equal(r.status, 0, r.stderr);
    const html = fs.readFileSync(path.join(f.out, 'index.html'), 'utf8');
    const panels = [...html.matchAll(/<article class="sc" id="([^"]+)"[^>]*>/g)];
    assert.deepEqual(panels.map((m) => m[1]), ['setup', 's1', 's2']);
    assert.doesNotMatch(panels[0][0], /\shidden/);
    assert.match(panels[1][0], /\shidden/);
    assert.match(panels[2][0], /\shidden/);
    assert.match(html, /<title>Held Facts Emulator Test<\/title>/);
  });

  await t.test('hidden panels stay hidden outside the Artifact host, whose CSS does that for the page', () => {
    const f = reportFixture();
    buildReport(f.json, f.out);
    const html = fs.readFileSync(path.join(f.out, 'index.html'), 'utf8');
    assert.match(html, /\[hidden\]\s*\{\s*display:\s*none\s*!important;?\s*\}/);
  });

  await t.test('declares UTF-8 first, so a plain file server shows “·” and quotes right', () => {
    const f = reportFixture();
    buildReport(f.json, f.out);
    const html = fs.readFileSync(path.join(f.out, 'index.html'), 'utf8');
    assert.match(html, /^<meta charset="utf-8">\n/);
  });

  await t.test('escapes the words and shows backticks as code', () => {
    const f = reportFixture();
    buildReport(f.json, f.out);
    const html = fs.readFileSync(path.join(f.out, 'index.html'), 'utf8');
    assert.match(html, /Tap &lt;Save&gt; &amp; &quot;go&quot;/);
    assert.match(html, /a &lt;b&gt;bold&lt;\/b&gt; claim/);
    assert.doesNotMatch(html, /<Save>/);
    assert.match(html, /Ran <span class="mono">emu\.sh pin<\/span>\./);
  });

  await t.test('copies the videos and screens beside the page and points at the copies', () => {
    const f = reportFixture();
    buildReport(f.json, f.out);
    assert.equal(fs.readFileSync(path.join(f.out, 'vid', 'step1.mp4'), 'utf8'), 'video bytes');
    assert.equal(fs.readFileSync(path.join(f.out, 'img', 'shot1.jpg'), 'utf8'), 'image bytes');
    const html = fs.readFileSync(path.join(f.out, 'index.html'), 'utf8');
    assert.match(html, /<video [^>]*src="vid\/step1\.mp4"/);
    assert.match(html, /<img src="img\/shot1\.jpg"/);
  });

  await t.test('a scenario without a video has no player', () => {
    const f = reportFixture();
    buildReport(f.json, f.out);
    const html = fs.readFileSync(path.join(f.out, 'index.html'), 'utf8');
    const setup = html.split('<article class="sc" id="setup"')[1].split('</article>')[0];
    assert.doesNotMatch(setup, /<video/);
  });

  await t.test('findings link to their scenario and refused requests stand out in the logs', () => {
    const f = reportFixture();
    buildReport(f.json, f.out);
    const html = fs.readFileSync(path.join(f.out, 'index.html'), 'utf8');
    assert.match(html, /<a href="#s1">Profile chip<\/a>/);
    assert.match(html, /<span class="bad"><span class="t">18:49:56<\/span>/);
  });

  await t.test('prints the files map for the Artifact publish', () => {
    const f = reportFixture();
    const r = buildReport(f.json, f.out);
    assert.deepEqual(JSON.parse(r.stdout), { 'vid/step1.mp4': 'vid/step1.mp4', 'img/shot1.jpg': 'img/shot1.jpg' });
  });

  await t.test('stops and names a file that is missing, writing no page', () => {
    const f = reportFixture();
    f.scenarios.scenarios[1].video.src = 'media/missing.mp4';
    fs.writeFileSync(f.json, JSON.stringify(f.scenarios));
    const r = buildReport(f.json, f.out);
    assert.equal(r.status, 1);
    assert.match(r.stderr, /missing\.mp4/);
    assert.equal(fs.existsSync(path.join(f.out, 'index.html')), false);
  });
});
