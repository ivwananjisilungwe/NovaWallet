#!/usr/bin/env node
// Batch runner: generate all NovaWallet screens in Stitch, sequentially.
// Reads every *.txt prompt in .planning/prompts, sorted by filename,
// and runs stitch-gen.mjs for each, appending results to a manifest.
import { readdirSync, readFileSync, writeFileSync, existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const PROMPT_DIR = join(__dirname, 'prompts');
const MANIFEST = '/tmp/stitch_manifest.json';

const files = readdirSync(PROMPT_DIR).filter(f => f.endsWith('.txt')).sort();
const manifest = existsSync(MANIFEST) ? JSON.parse(readFileSync(MANIFEST, 'utf8')) : { screens: [] };
const done = new Set(manifest.screens.map(s => s.slug));

function runOne(slug, promptFile) {
  return new Promise((resolve) => {
    const p = spawn('node', [join(__dirname, 'stitch-gen.mjs'), slug, promptFile], { stdio: ['ignore', 'pipe', 'pipe'] });
    let out = '';
    p.stdout.on('data', d => out += d);
    p.stderr.on('data', d => out += d);
    p.on('close', code => {
      let summary = null;
      try { summary = JSON.parse(readFileSync(`/tmp/stitch_${slug}.json`, 'utf8')); } catch {}
      manifest.screens.push({ slug, file: promptFile, ok: code === 0 && summary?.ok, isError: summary?.isError, screenNames: summary?.screenNames || [], exit: code, log: out.slice(-500) });
      writeFileSync(MANIFEST, JSON.stringify(manifest, null, 2));
      resolve();
    });
  });
}

(async () => {
  for (const f of files) {
    const slug = f.replace(/\.txt$/, '');
    if (done.has(slug)) { console.log(`skip ${slug} (already done)`); continue; }
    console.log(`>>> generating ${slug}`);
    await runOne(slug, join(PROMPT_DIR, f));
    const last = manifest.screens[manifest.screens.length - 1];
    console.log(`<<< ${slug} ok=${last?.ok} screens=${JSON.stringify(last?.screenNames)}`);
  }
  const okc = manifest.screens.filter(s => s.ok).length;
  console.log(`ALL DONE. ${okc}/${manifest.screens.length} succeeded. Manifest: ${MANIFEST}`);
})();
