#!/usr/bin/env node
// MCP (Model Context Protocol) stdio client.
// Usage: node mcp-connect.mjs <serverName> [-- extraArgs...]
// Reads servers from MCP_SERVERS_JSON (default: ~/.cline/data/settings/cline_mcp_settings.json).
import { spawn } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { homedir } from 'node:os';
import { join } from 'node:path';

const serverName = process.argv[2];
if (!serverName) {
  console.error('Usage: node mcp-connect.mjs <serverName> [-- extraArgs...]');
  process.exit(1);
}
const dd = process.argv.indexOf('--');
const extraArgs = dd === -1 ? [] : process.argv.slice(dd + 1);

const configPath =
  process.env.MCP_SERVERS_JSON ||
  join(homedir(), '.cline', 'data', 'settings', 'cline_mcp_settings.json');
const config = JSON.parse(readFileSync(configPath, 'utf8'));
const def = (config.mcpServers || {})[serverName];
if (!def) {
  console.error(`MCP server "${serverName}" not found in ${configPath}`);
  process.exit(1);
}

if (def.type && def.type !== 'stdio') {
  console.error(`Server "${serverName}" is not stdio (type=${def.type}); only stdio is supported here.`);
  process.exit(1);
}

const command = def.command;
const args = [...(def.args || []), ...extraArgs];
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
child.stderr && child.stderr.on('data', (d) => process.stderr.write(d));
child.on('error', (e) => { console.error('[spawn-error]', e.message); process.exit(1); });

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function main() {
  const init = await request('initialize', {
    protocolVersion: '2024-11-05',
    capabilities: {},
    clientInfo: { name: 'novawallet-connector', version: '1.0.0' },
  });
  console.log(`\n[connected] "${serverName}"  →  ${command} ${args.join(' ')}`);
  console.log(`[server] ${init.serverInfo?.name || '?'} v${init.serverInfo?.version || '?'}`);
  console.log(`[protocol] ${init.protocolVersion}`);
  send({ jsonrpc: '2.0', method: 'notifications/initialized' });
  await sleep(400);

  const tools = await request('tools/list', {});
  const list = tools.tools || [];
  console.log(`\n[${serverName}] exposes ${list.length} tool(s):`);
  for (const t of list) {
    const desc = (t.description || '').split('\n')[0];
    console.log(`  - ${t.name}: ${desc}`);
  }

  // Optional: invoke a specific tool: <serverName> <toolName> '[jsonParams]'
  const toolName = process.argv[3];
  if (toolName) {
    let params = {};
    if (process.argv[4]) {
      try { params = JSON.parse(process.argv[4]); }
      catch (e) { console.error(`\n[call-error] invalid JSON params: ${e.message}`); child.kill(); process.exit(1); }
    }
    if (!list.some((t) => t.name === toolName)) {
      console.error(`\n[call-error] tool "${toolName}" not found.`);
      child.kill(); process.exit(1);
    }
    const schemaTool = list.find((t) => t.name === toolName);
    if (schemaTool?.inputSchema) {
      console.log(`\n[schema] ${toolName} inputSchema: ${JSON.stringify(schemaTool.inputSchema)}`);
    }
    console.log(`\n[call] ${toolName} ${JSON.stringify(params)}`);
    try {
      const res = await request('tools/call', { name: toolName, arguments: params });
      console.log(`\n[result] ${JSON.stringify(res, null, 2)}`);
    } catch (e) {
      console.error(`\n[call-error] ${e.message}`);
    }
  }
  child.kill();
  process.exit(0);
}

main().catch((e) => {
  console.error('\n[connect-error]', e.message);
  child.kill();
  process.exit(1);
});
