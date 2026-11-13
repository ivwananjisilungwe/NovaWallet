# OPS-02 — Server hardening, Caddy, TLS, domains 🔒

**Area:** infra · **Needs:** OPS-01 · **Decisions:** DEC-19 (hosting, domain)

---
Read `infra/AGENTS.md`. You write scripts and documents; **the owner runs them on the server.** Never ask for or handle server credentials.

Do:
1. `infra/server/bootstrap.sh` (idempotent, Ubuntu LTS): create a deploy user, SSH key-only + no root login, firewall default-deny (allow 22 from owner IPs if provided, 80, 443), automatic security updates, fail2ban, time sync, swap for small VMs, Docker + compose plugin, log rotation, unattended reboot window. `shellcheck` clean, every step explained in `infra/server/README.md`.
2. **Caddyfile** (`infra/caddy/`): `api.<domain>` → blue/green upstream with active health checks; `admin.<domain>` → static admin build with strict CSP; automatic HTTPS; HSTS; security headers; request size limits (match the 10 MB KYC cap); rate limiting at the edge if available; `/api/v1/admin/**` optionally restricted by IP allow-list; actuator and docs paths blocked publicly; access logs JSON without query strings/PII; trusted-proxy headers set so the API sees the real client IP (BE-1C-7).
3. Webhook paths reachable only from the internet over TLS, with body size limits; document provider IP allow-lists as optional.
4. DNS/TLS checklist for the owner (A records, CAA, verifying certificates).
5. Separate staging and production servers (or at minimum separate compose projects + databases + secrets); staging protected by basic auth or IP allow-list.
6. `infra/server/HARDENING_CHECKLIST.md` the owner ticks after running the scripts (with the commands to verify each item).

Gates: `caddy validate` in a container; shellcheck; a local compose dry-run of caddy + two API containers showing health-based switching.
