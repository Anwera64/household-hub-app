#!/usr/bin/env node
// Builds a manual test's report page from a scenarios file: a summary, then one panel per scenario
// (Setup, each step, extras, Cleanup) holding its video, what should and did show, screens, logs and
// database checks, one at a time with a list, Previous/Next, the arrow keys and #id links.
//
//   node report.mjs build <scenarios.json> <outdir>
//
// Writes <outdir>/index.html, copies every video into <outdir>/vid/ and every image into
// <outdir>/img/, and prints the files map for the Artifact tool's publish (root = <outdir>,
// file_path = <outdir>/index.html, files = the map). A missing file stops the build and names it.
//
// scenarios.json (paths are relative to the file; `backticks` in prose become code):
// {
//   "title": "Held Facts Emulator Test",                 page name, 2–4 words
//   "eyebrow": "home/household-hub-app#39 · PR #49 · Android",
//   "intro": "One paragraph: what ran where, and the verdict.",
//   "meta": [{ "label": "Branch", "value": "feat/39 @ cb3696f", "href": "optional" }],
//   "tiles": [{ "n": "8 / 8", "l": "manual test steps passed" }],
//   "findings": [{ "kind": "pre", "label": "Pre-existing · #41", "text": "…", "scenario": "s7" }],
//   "scenarios": [{
//     "id": "s1", "key": "1", "name": "Profile chip",     id for #links, key and name for the list
//     "title": "1 · Profile: the Memories chip counts held facts",
//     "badges": [{ "kind": "pass", "label": "Pass" }],      the first is the result
//     "video": { "src": "../step1.mp4", "caption": "step1 · 8.8 s" },
//     "facts": [{ "label": "Should show", "text": "…" },   or "items": [...] (bullets),
//               { "label": "Did", "steps": ["…"] }],        or "steps": [...] (numbered)
//     "callouts": [{ "kind": "pre", "title": "…", "text": "…", "link": { "href": "…", "text": "#41" } }],
//     "shots": [{ "src": "img.jpg", "alt": "…", "caption": "…", "wide": false }],
//     "logs": [{ "title": "Hub access log",
//                "lines": [{ "t": "18:43:56", "text": "GET /api/v1/memories/page  200", "note": "…", "bad": false }] }]
//   }],
//   "notCovered": ["…"],
//   "notes": { "title": "…", "items": ["…"] },
//   "files": "Where the originals are."
// }
// Kinds: pass, fail, ask (a decision for the reader), note, pre (pre-existing, or done).
import fs from 'node:fs';
import path from 'node:path';

const esc = (s) =>
  String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
const txt = (s) => esc(s).replace(/`([^`]+)`/g, '<span class="mono">$1</span>');
const link = (l) => (l ? ` <a href="${esc(l.href)}">${txt(l.text)}</a>` : '');
const KINDS = new Set(['pass', 'fail', 'ask', 'note', 'pre']);
const kind = (k) => (KINDS.has(k) ? k : 'note');

function build(jsonPath, outDir) {
  const base = path.dirname(path.resolve(jsonPath));
  const data = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
  const copies = new Map(); // published path -> source
  const problems = [];

  const media = (src, folder) => {
    const from = path.resolve(base, src);
    if (!fs.existsSync(from)) {
      problems.push(`missing: ${src}`);
      return '';
    }
    const to = `${folder}/${path.basename(from)}`;
    if (copies.has(to) && copies.get(to) !== from) problems.push(`two files would be ${to}: ${copies.get(to)} and ${from}`);
    copies.set(to, from);
    return to;
  };

  const scenarios = data.scenarios || [];
  const ids = new Set();
  for (const s of scenarios) {
    if (!/^[A-Za-z0-9-]+$/.test(s.id || '')) problems.push(`scenario id "${s.id}" must be letters, digits or hyphens`);
    if (ids.has(s.id)) problems.push(`scenario id "${s.id}" is used twice`);
    ids.add(s.id);
  }

  const panels = scenarios.map((s, i) => panel(s, i, media)).join('\n');
  if (problems.length) {
    process.stderr.write(`${problems.join('\n')}\n`);
    process.exit(1);
  }

  const names = Object.fromEntries(scenarios.map((s) => [s.id, s.name]));
  const html = page(data, panels, names);
  fs.mkdirSync(outDir, { recursive: true });
  for (const [to, from] of copies) {
    fs.mkdirSync(path.join(outDir, path.dirname(to)), { recursive: true });
    fs.copyFileSync(from, path.join(outDir, to));
  }
  fs.writeFileSync(path.join(outDir, 'index.html'), html);
  process.stdout.write(`${JSON.stringify(Object.fromEntries([...copies.keys()].map((k) => [k, k])), null, 2)}\n`);
  process.stderr.write(`${path.join(outDir, 'index.html')}: ${scenarios.length} scenarios, ${copies.size} files\n`);
}

function state(s) {
  const kinds = (s.badges || []).map((b) => b.kind);
  if (kinds.includes('fail')) return 'fail';
  if (kinds.includes('ask')) return 'ask';
  return ['pre', 'note'].includes(kinds[0]) ? 'info' : 'pass';
}

function facts(list = []) {
  const dd = (f) => {
    if (f.steps) return `<ol>${f.steps.map((x) => `<li>${txt(x)}</li>`).join('')}</ol>`;
    if (f.items) return `<ul>${f.items.map((x) => `<li>${txt(x)}</li>`).join('')}</ul>`;
    return txt(f.text);
  };
  if (!list.length) return '';
  return `<dl>${list.map((f) => `<div><dt>${txt(f.label)}</dt><dd>${dd(f)}</dd></div>`).join('')}</dl>`;
}

function logLine(l) {
  const parts = [];
  if (l.t) parts.push(`<span class="t">${esc(l.t)}</span>`);
  if (l.text) parts.push(esc(l.text));
  if (l.note) parts.push(`<span class="c">${esc(l.note)}</span>`);
  const line = parts.join('  ');
  return l.bad ? `<span class="bad">${line}</span>` : line;
}

function panel(s, i, media) {
  const id = esc(s.id);
  const badges = (s.badges || []).map((b) => `<span class="pill ${kind(b.kind)}">${txt(b.label)}</span>`).join('');
  const video = s.video
    ? `<div><video controls preload="metadata" playsinline src="${media(s.video.src, 'vid')}"></video>${
        s.video.caption ? `<p class="vcap">${txt(s.video.caption)}</p>` : ''
      }</div>`
    : '';
  const callouts = (s.callouts || [])
    .map((c) => `<div class="callout ${kind(c.kind)}"><b>${txt(c.title)}${link(c.link)}</b>${c.text ? `<span>${txt(c.text)}</span>` : ''}</div>`)
    .join('');
  const figure = (f) => {
    const src = media(f.src, 'img');
    return `<figure><a href="${src}" target="_blank" rel="noopener"><img src="${src}" alt="${esc(f.alt || f.caption)}" loading="lazy"></a>${
      f.caption ? `<figcaption>${txt(f.caption)}</figcaption>` : ''
    }</figure>`;
  };
  const shots = s.shots || [];
  const narrow = shots.filter((f) => !f.wide).map(figure).join('');
  const wide = shots.filter((f) => f.wide).map(figure).join('');
  const screens = shots.length
    ? `<div class="ev"><h4>Screens</h4>${narrow ? `<div class="shots">${narrow}</div>` : ''}${wide ? `<div class="shots wide">${wide}</div>` : ''}</div>`
    : '';
  const logs = (s.logs || [])
    .map((g) => `<div class="ev"><h4>${txt(g.title)}</h4><pre class="log">${(g.lines || []).map(logLine).join('\n')}</pre></div>`)
    .join('');
  return `<article class="sc" id="${id}" data-k="${esc(s.key ?? i)}" data-name="${esc(s.name || s.id)}" data-state="${state(s)}" aria-labelledby="h-${id}"${i ? ' hidden' : ''}>
<div class="sc-head"><h3 id="h-${id}">${txt(s.title || s.name)}</h3>${badges}</div>
<div class="sc-main${video ? '' : ' novid'}">${video}<div class="facts">${facts(s.facts)}${callouts}</div></div>
${screens}${logs}
</article>`;
}

function page(d, panels, names) {
  const meta = (d.meta || [])
    .map((m) => `<span>${txt(m.label)} ${m.href ? `<a href="${esc(m.href)}">${txt(m.value)}</a>` : `<b>${txt(m.value)}</b>`}</span>`)
    .join('');
  const tiles = (d.tiles || []).map((x) => `<div class="tile"><span class="n">${txt(x.n)}</span><span class="l">${txt(x.l)}</span></div>`).join('');
  const findings = (d.findings || [])
    .map((f) => `<li><span class="pill ${kind(f.kind)}">${txt(f.label)}</span><span class="t">${txt(f.text)}</span>${
      f.scenario ? `<a href="#${esc(f.scenario)}">${txt(names[f.scenario] || f.scenario)}</a>` : ''
    }</li>`)
    .join('');
  const notCovered = (d.notCovered || []).length
    ? `<section aria-labelledby="gap"><h2 id="gap">Not covered here</h2><ul class="plain">${d.notCovered.map((x) => `<li>${txt(x)}</li>`).join('')}</ul></section>`
    : '';
  const notes = d.notes
    ? `<details><summary>${txt(d.notes.title)}</summary><ul class="plain">${(d.notes.items || []).map((x) => `<li>${txt(x)}</li>`).join('')}</ul></details>`
    : '';
  const files = d.files ? `<section aria-labelledby="files"><h2 id="files">Files</h2><p>${txt(d.files)}</p></section>` : '';
  // The Artifact host adds its own head; the charset first keeps a plain file server right too.
  return `<meta charset="utf-8">
<title>${esc(d.title)}</title>
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Outfit:wght@500;600&family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@500&display=swap">
<style>${CSS}</style>
<div class="wrap">
<header class="top">
${d.eyebrow ? `<span class="eyebrow">${txt(d.eyebrow)}</span>` : ''}
<h1>${txt(d.title)}</h1>
${d.intro ? `<p>${txt(d.intro)} Use the list, Previous and Next, or the arrow keys.</p>` : ''}
${meta ? `<div class="meta">${meta}</div>` : ''}
</header>
<section aria-labelledby="sum"><h2 id="sum">Summary</h2>
${tiles ? `<div class="verdict">${tiles}</div>` : ''}
${findings ? `<ul class="flist">${findings}</ul>` : ''}
</section>
<section aria-labelledby="scen"><h2 id="scen">Scenarios</h2>
<div class="viewer">
<nav class="nav" aria-label="Scenarios" id="nav"></nav>
<div class="panel">
<div class="pager"><button type="button" class="btn" id="prev">← Previous</button><span class="count" id="count" aria-live="polite"></span><button type="button" class="btn" id="next">Next →</button></div>
${panels}
<div class="pager"><button type="button" class="btn" id="prev2">← Previous</button><span class="count" id="count2"></span><button type="button" class="btn" id="next2">Next →</button></div>
</div>
</div>
</section>
${notCovered}
${notes}
${files}
</div>
<script>${SCRIPT}</script>
`;
}

// The look of the #39 report (the app's Hearth palette), light and dark.
const CSS = `
:root {
  --bg: #F5F2EB; --surface: #FFFFFF; --surface-alt: #FAF8F3; --fg: #221F1E; --muted: #5A534E; --line: #E6E0D5;
  --accent: #3C6E4E; --sel: #E3ECE5;
  --pass-bg: #D0DFD5; --pass-fg: #102318; --fail-bg: #F8D5CD; --fail-fg: #5A1708; --ask-bg: #FFE3B8; --ask-fg: #4A2E00;
  --note-bg: #E4E0F2; --note-fg: #26203F; --pre-bg: #ECE8E1; --pre-fg: #3A3532; --bad: #B4472F;
  --font-display: 'Outfit', 'Segoe UI', system-ui, sans-serif;
  --font-body: 'Inter', 'Segoe UI', system-ui, sans-serif;
  --font-mono: 'JetBrains Mono', ui-monospace, 'Cascadia Mono', Consolas, monospace;
}
@media (prefers-color-scheme: dark) { :root:not([data-theme="light"]) {
  --bg: #161514; --surface: #1F1D1B; --surface-alt: #252220; --fg: #EDE8E0; --muted: #B5ADA4; --line: #34302C;
  --accent: #8CC5A0; --sel: #26352B; --pass-bg: #23392C; --pass-fg: #CDE7D5; --fail-bg: #4A1E15; --fail-fg: #FFD2C7;
  --ask-bg: #4A3511; --ask-fg: #FFE1AE; --note-bg: #2F2A45; --note-fg: #DDD6F5; --pre-bg: #2A2724; --pre-fg: #D9D2C9;
  --bad: #FF9D85; color-scheme: dark; } }
:root[data-theme="dark"] {
  --bg: #161514; --surface: #1F1D1B; --surface-alt: #252220; --fg: #EDE8E0; --muted: #B5ADA4; --line: #34302C;
  --accent: #8CC5A0; --sel: #26352B; --pass-bg: #23392C; --pass-fg: #CDE7D5; --fail-bg: #4A1E15; --fail-fg: #FFD2C7;
  --ask-bg: #4A3511; --ask-fg: #FFE1AE; --note-bg: #2F2A45; --note-fg: #DDD6F5; --pre-bg: #2A2724; --pre-fg: #D9D2C9;
  --bad: #FF9D85; color-scheme: dark; }
* { box-sizing: border-box; }
[hidden] { display: none !important; }
body { background: var(--bg); color: var(--fg); font-family: var(--font-body); font-size: 15px; line-height: 1.55; }
.wrap { max-width: 1180px; margin: 0 auto; padding-block: 36px 72px; padding-left: max(16px, env(safe-area-inset-left, 0px)); padding-right: max(16px, env(safe-area-inset-right, 0px)); display: flex; flex-direction: column; gap: 36px; }
a { color: var(--accent); }
:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
h1, h2, h3, h4 { font-family: var(--font-display); font-weight: 600; text-wrap: balance; margin: 0; letter-spacing: -0.01em; }
h1 { font-size: clamp(28px, 5vw, 38px); line-height: 1.1; }
h2 { font-size: 22px; }
h3 { font-size: 20px; }
h4 { font-size: 13px; font-family: var(--font-body); letter-spacing: 0.08em; text-transform: uppercase; color: var(--muted); }
p { margin: 0; max-width: 72ch; }
.mono { font-family: var(--font-mono); font-size: 0.86em; }
.eyebrow { font-size: 12px; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase; color: var(--muted); }
header.top { display: flex; flex-direction: column; gap: 12px; }
.meta { display: flex; flex-wrap: wrap; gap: 6px 20px; color: var(--muted); font-size: 13px; }
.meta b { color: var(--fg); font-weight: 500; }
section { display: flex; flex-direction: column; gap: 14px; }
.verdict { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; }
.tile { background: var(--surface); border: 1px solid var(--line); border-radius: 16px; padding: 14px 16px; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.tile .n { font-family: var(--font-display); font-size: 26px; font-weight: 600; font-variant-numeric: tabular-nums; }
.tile .l { font-size: 13px; color: var(--muted); }
.pill { display: inline-flex; align-items: center; padding: 2px 10px; border-radius: 999px; font-size: 12px; font-weight: 600; white-space: nowrap; }
.pass { background: var(--pass-bg); color: var(--pass-fg); }
.fail { background: var(--fail-bg); color: var(--fail-fg); }
.ask { background: var(--ask-bg); color: var(--ask-fg); }
.note { background: var(--note-bg); color: var(--note-fg); }
.pre { background: var(--pre-bg); color: var(--pre-fg); }
.flist { display: flex; flex-direction: column; gap: 8px; margin: 0; padding: 0; list-style: none; }
.flist li { display: flex; flex-wrap: wrap; align-items: baseline; gap: 6px 10px; background: var(--surface); border: 1px solid var(--line); border-radius: 14px; padding: 12px 16px; }
.flist li span.t { flex: 1 1 320px; min-width: 0; }
.viewer { display: grid; grid-template-columns: 250px minmax(0, 1fr); gap: 20px; align-items: start; }
.nav { position: sticky; top: calc(env(safe-area-inset-top, 0px) + 16px); display: flex; flex-direction: column; gap: 4px; background: var(--surface); border: 1px solid var(--line); border-radius: 18px; padding: 8px; }
.nav button { display: grid; grid-template-columns: 30px minmax(0, 1fr) auto; align-items: center; gap: 8px; width: 100%; min-height: 44px; padding: 6px 10px; border: 0; border-radius: 12px; background: transparent; color: var(--fg); font: inherit; font-size: 14px; text-align: left; cursor: pointer; }
.nav button:hover { background: var(--surface-alt); }
.nav button[aria-current="true"] { background: var(--sel); font-weight: 600; }
.nav .k { font-family: var(--font-mono); font-size: 12px; color: var(--muted); }
.nav .dot { width: 9px; height: 9px; border-radius: 50%; background: var(--pass-fg); opacity: .55; }
.nav .dot.ask { background: #C98A1B; opacity: 1; }
.nav .dot.fail { background: var(--bad); opacity: 1; }
.nav .dot.info { background: var(--muted); opacity: .5; }
.panel { min-width: 0; display: flex; flex-direction: column; gap: 18px; }
.pager { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.pager .count { font-size: 13px; color: var(--muted); font-variant-numeric: tabular-nums; }
.btn { min-height: 44px; padding: 0 16px; border-radius: 12px; border: 1px solid var(--line); background: var(--surface); color: var(--fg); font: inherit; font-weight: 600; font-size: 14px; cursor: pointer; }
.btn:hover:not(:disabled) { border-color: var(--accent); }
.btn:disabled { opacity: .45; cursor: default; }
article.sc { background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 22px; display: flex; flex-direction: column; gap: 22px; min-width: 0; }
.sc-head { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 12px; }
.sc-head h3 { flex: 1 1 260px; }
.sc-main { display: grid; grid-template-columns: minmax(0, 320px) minmax(0, 1fr); gap: 24px; align-items: start; }
.sc-main.novid { grid-template-columns: minmax(0, 1fr); }
video { width: 100%; max-width: 320px; height: auto; border-radius: 16px; border: 1px solid var(--line); background: #000; display: block; }
.vcap { font-size: 12px; color: var(--muted); margin-top: 6px; }
.facts { display: flex; flex-direction: column; gap: 16px; min-width: 0; }
.facts dl { margin: 0; display: grid; gap: 12px; }
.facts dt { font-size: 12px; font-weight: 600; letter-spacing: 0.08em; text-transform: uppercase; color: var(--muted); }
.facts dd { margin: 4px 0 0; }
.facts ol, .facts ul { margin: 4px 0 0; padding-left: 20px; display: flex; flex-direction: column; gap: 4px; }
.callout { border-radius: 14px; padding: 12px 14px; display: flex; flex-direction: column; gap: 4px; }
.callout b { font-weight: 600; }
.ev { display: flex; flex-direction: column; gap: 10px; min-width: 0; }
.shots { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 14px; }
.shots.wide { grid-template-columns: minmax(0, 1fr); }
figure { margin: 0; display: flex; flex-direction: column; gap: 6px; min-width: 0; }
figure a { display: block; }
figure img { width: 100%; max-width: 100%; height: auto; border-radius: 12px; border: 1px solid var(--line); display: block; }
figcaption { font-size: 12.5px; color: var(--muted); }
.log { overflow-x: auto; background: var(--surface-alt); border: 1px solid var(--line); border-radius: 12px; padding: 12px 14px; font-family: var(--font-mono); font-size: 12px; line-height: 1.65; white-space: pre; color: var(--fg); margin: 0; }
.log .t, .log .c { color: var(--muted); }
.log .bad { color: var(--bad); }
ul.plain { margin: 0; padding-left: 20px; display: flex; flex-direction: column; gap: 6px; max-width: 80ch; }
details { background: var(--surface); border: 1px solid var(--line); border-radius: 16px; padding: 14px 18px; }
summary { cursor: pointer; font-family: var(--font-display); font-weight: 600; font-size: 16px; min-height: 28px; }
details[open] summary { margin-bottom: 12px; }
@media (max-width: 860px) {
  .viewer { grid-template-columns: minmax(0, 1fr); }
  .nav { position: static; flex-direction: row; overflow-x: auto; padding: 6px; }
  .nav button { flex: none; width: auto; grid-template-columns: auto auto; }
  .nav button .dot { display: none; }
  .sc-main { grid-template-columns: minmax(0, 1fr); }
  video { max-width: 280px; }
  article.sc { padding: 16px; }
}
@media (prefers-reduced-motion: reduce) { * { scroll-behavior: auto !important; } }
`;

// One scenario at a time: the list, Previous/Next, the arrow keys, and #id in the address.
const SCRIPT = `
(function () {
  var panels = Array.prototype.slice.call(document.querySelectorAll('article.sc'));
  if (!panels.length) return;
  var ids = panels.map(function (p) { return p.id; });
  var nav = document.getElementById('nav');
  var buttons = {};
  panels.forEach(function (p) {
    var b = document.createElement('button');
    b.type = 'button';
    b.setAttribute('aria-controls', p.id);
    var k = document.createElement('span'); k.className = 'k'; k.textContent = p.dataset.k;
    var n = document.createElement('span'); n.textContent = p.dataset.name;
    var st = p.dataset.state;
    var d = document.createElement('span'); d.className = 'dot' + (st === 'pass' ? '' : ' ' + st);
    d.setAttribute('aria-hidden', 'true');
    b.appendChild(k); b.appendChild(n); b.appendChild(d);
    b.addEventListener('click', function () { go(p.id, true); });
    nav.appendChild(b);
    buttons[p.id] = b;
  });
  var current = null;
  function show(id) {
    if (ids.indexOf(id) < 0) id = ids[0];
    current = id;
    panels.forEach(function (p) {
      var on = p.id === id;
      if (!on) { p.querySelectorAll('video').forEach(function (v) { try { v.pause(); } catch (e) {} }); }
      p.hidden = !on;
      buttons[p.id].setAttribute('aria-current', on ? 'true' : 'false');
    });
    var i = ids.indexOf(id);
    var label = (i + 1) + ' of ' + ids.length;
    document.getElementById('count').textContent = label;
    document.getElementById('count2').textContent = label;
    ['prev', 'prev2'].forEach(function (x) { document.getElementById(x).disabled = i === 0; });
    ['next', 'next2'].forEach(function (x) { document.getElementById(x).disabled = i === ids.length - 1; });
    var b = buttons[id];
    if (b && b.scrollIntoView && window.matchMedia('(max-width: 860px)').matches) {
      try { b.scrollIntoView({ block: 'nearest', inline: 'center' }); } catch (e) {}
    }
  }
  function go(id, scroll) {
    if (location.hash !== '#' + id) { location.hash = id; } else { show(id); }
    if (scroll) {
      var v = document.querySelector('.viewer');
      if (v.getBoundingClientRect().top < 0) v.scrollIntoView({ block: 'start' });
    }
  }
  function step(d) {
    var i = ids.indexOf(current) + d;
    if (i >= 0 && i < ids.length) go(ids[i], true);
  }
  ['prev', 'prev2'].forEach(function (x) { document.getElementById(x).addEventListener('click', function () { step(-1); }); });
  ['next', 'next2'].forEach(function (x) { document.getElementById(x).addEventListener('click', function () { step(1); }); });
  document.addEventListener('keydown', function (e) {
    if (e.altKey || e.ctrlKey || e.metaKey || e.shiftKey) return;
    var t = e.target && e.target.tagName;
    if (t === 'VIDEO' || t === 'INPUT' || t === 'TEXTAREA' || t === 'SELECT') return;
    if (e.key === 'ArrowRight') step(1);
    else if (e.key === 'ArrowLeft') step(-1);
  });
  window.addEventListener('hashchange', function () { show(location.hash.slice(1)); });
  show(location.hash.slice(1));
})();
`;

const [cmd, jsonPath, outDir] = process.argv.slice(2);
if (cmd !== 'build' || !jsonPath || !outDir) {
  process.stderr.write('usage: node report.mjs build <scenarios.json> <outdir>\n');
  process.exit(2);
}
build(jsonPath, outDir);
