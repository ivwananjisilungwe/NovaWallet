# BE-1C-7 — Phase 1C Batch 7: Rate limiting & abuse protection 🔒

**Area:** novawallet-api · **Needs:** BE-1C-6 · **Decisions:** DEC-09

---
Follow `_COMMON_BATCH_RULES.md` for Batch 7.

Scope (plan §7 Batch 7): SEC-09.

Do:
1. Inventory existing limiters (login, velocity, daily-send). Document each: key, window, limit, where enforced.
2. Per-operation limits behind one `RateLimiter` interface (in-memory implementation now; shared-store implementation later without touching callers):
   - login, register, refresh, password reset/forgot, email verification, OTP send/verify (when added), PIN attempts — keyed by account **and** by client IP, with progressive backoff and lockout;
   - money operations — per user and per wallet (complements the existing velocity limits);
   - lookups (user lookup, name check) — strict, to prevent enumeration;
   - uploads and webhooks — sane caps.
3. Proxy-aware client IP: trust `X-Forwarded-For` only from configured proxy addresses (Caddy); default to remote address. Test spoofing.
4. Responses: `429` with `Retry-After`, consistent error envelope, no hint whether an account exists.
5. PIN/login lockouts: counters stored durably (DB), not only in memory, so a restart does not reset an attacker's attempts. Unlock path documented.
6. All limits configurable per environment (properties), with safe defaults. Document the single-instance limitation (DEC-09) in `SECURITY.md`.

Tests: limit reached → 429, window reset, per-key isolation, spoofed header ignored, lockout survives restart (repository test), no enumeration difference in responses/timing class.
