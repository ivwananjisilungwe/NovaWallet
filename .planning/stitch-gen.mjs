#!/usr/bin/env node
// Stitch MCP helper: generate a screen from a prompt file.
// Usage: node stitch-gen.mjs <slug> <promptFile> [-- <extraToolArgs>]
// Reads the prompt text from <promptFile>, calls generate_screen_from_text
// with { projectId, prompt }, and writes the raw bridge log + a parsed
// summary to /tmp/stitch_<slug>.txt and /tmp/stitch_<slug>.json
import { spawn } from 'node:child_process';
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { homedir } from 'node:os';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const PROJECT_ID = process.env.STITCH_PROJECT_ID || '11542754846512650236';
const DESIGN_SYSTEM = process.env.STITCH_DESIGN_SYSTEM || 'assets/3eff015a7c124ead8b3372f51bc4572c';

const slug = process.argv[2];
const promptFile = process.argv[3];
if (!slug || !promptFile) {
  console.error('Usage: node stitch-gen.mjs <slug> <promptFile>');
  process.exit(1);
}
const prompt = readFileSync(promptFile, 'utf8').trim();

const configPath = join(homedir(), '.cline', 'data', 'settings', 'cline_mcp_settings.json');
const config = JSON.parse(readFileSync(configPath, 'utf8'));
const def = (config.mcpServers || {}).stitch;
const command = def.command;
const args = [...(def.args || [])];
const env = { ...process.env, ...(def.env || {}) };
const child = spawn(command, args, { env, stdio: ['pipe', 'pipe', 'inherit'] });

let buf = '';
let nextId = 0;
const pending = new Map();
function send(msg) { child.stdin.write(JSON.stringify(msg) + '\n'); }
function request(method, params = {}) {
  const id = ++nextId;
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject });
    send({ jsonrpc: '2.0', id, method, params });
  });
}
child.stdout.on('data', (chunk) => {
  buf += chunk.toString();
  let idx;
  while ((idx = buf.indexOf('\n')) !== -1) {
    const line = buf.slice(0, idx).trim();
    buf = buf.slice(idx + 1);
    if (!line) continue;
    let msg;
    try { msg = JSON.parse(line); } catch { continue; }
    if (msg.id && pending.has(msg.id)) {
      const p = pending.get(msg.id);
      pending.delete(msg.id);
      msg.error ? p.reject(new Error(JSON.stringify(msg.error))) : p.resolve(msg.result);
    }
  }
});
child.on('error', (e) => { console.error('[spawn-error]', e.message); process.exit(1); });

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const outLog = `/tmp/stitch_${slug}.txt`;
const log = [];
function tee(s) { log.push(s); }

async function main() {
  const init = await request('initialize', {
    protocolVersion: '2024-11-05', capabilities: {},
    clientInfo: { name: 'novawallet-gen', version: '1.0.0' },
  });
  tee(`[connected] stitch -> ${init.serverInfo?.name || '?'}`);
  send({ jsonrpc: '2.0', method: 'notifications/initialized' });
  await sleep(300);

  const params = { projectId: PROJECT_ID, prompt, designSystem: DESIGN_SYSTEM, deviceType: 'MOBILE' };
  tee(`[call] generate_screen_from_text slug=${slug} promptLen=${prompt.length}`);
  const res = await request('tools/call', { name: 'generate_screen_from_text', arguments: params });
  const text = res.content?.map((c) => c.text || '').join('\n');
  // try to parse structured + text for screen names
  let screenNames = [];
  for (const m of text.matchAll(/projects\/\d+\/screens\/[a-f0-9]+/g)) screenNames.push(m[0]);
  screenNames = [...new Set(screenNames)];
  const summary = {
    slug, ok: !res.isError, isError: !!res.isError,
    screenNames, sessionId: res.sessionId, projectId: res.projectId,
    textPreview: text.slice(0, 400),
  };
  writeFileSync(`/tmp/stitch_${slug}.json`, JSON.stringify(summary, null, 2));
  tee(`[done] isError=${summary.isError} screens=${JSON.stringify(screenNames)}`);
  writeFileSync(outLog, log.join('\n') + '\n');
  child.kill();
  process.exit(0);
}
main().catch((e) => {
  writeFileSync(outLog, log.join('\n') + '\n[error] ' + e.message + '\n');
  child.kill();
  process.exit(1);
});
