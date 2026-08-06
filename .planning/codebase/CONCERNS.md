# Concerns & Risks

## Current Known Concerns
1. **Real Mobile‑Money Integration**
   - The backend currently uses a **mock** MobileMoneyRequest workflow. Production integration with Airtel/Mtn APIs is slated for Phase 2. Until then, manual admin confirmation is required, which is a bottleneck.
2. **Frontend Test Deficit**
   - The Flutter client has only the default widget test. No unit or integration tests exist for the Riverpod providers, API client, or UI flows. This reduces confidence in the mobile UI and hampers CI quality gates.
3. **Large Service Classes**
   - `TransactionService` (≈443 LOC) and `AdminController` (≈436 LOC) were flagged as potential god‑classes. They have been refactored into smaller cohesive units, but further decomposition may be needed as new features (e.g., fraud detection) are added.
4. **Secret Fallbacks**
   - The JWT secret fallback (`${JWT_SECRET}`) currently defaults to a hard‑coded development value if not provided. In production, the environment variable must be mandatory; otherwise the app would start with a weak secret.
5. **Rate‑Limiting Granularity**
   - Rate limiting is applied globally via `LoginRateLimiter` and a generic `RateLimitFilter`. More fine‑grained limits per endpoint (e.g., transfer attempts) could improve security.
6. **Documentation Gaps**
   - While high‑level specs and user‑flow documents exist, there is limited API contract documentation for external developers (e.g., OpenAPI docs are generated but not published). Adding a public API spec would aid third‑party integration.

## Future Risks
- **Scaling the Database**: As transaction volume grows, the current single‑node PostgreSQL deployment may need read replicas and connection pooling tuning.
- **Compliance**: KYC handling stores document URLs in Cloudinary. GDPR/PDPA compliance requires proper data‑subject rights handling – not yet addressed.
- **Security Audits**: Although audit logging is in place, periodic security reviews (e.g., OWASP scans) are not automated.

## Mitigation Actions (Suggested)
- Implement real mobile‑money provider adapters and add end‑to‑end tests.
- Expand Flutter test suite to cover core providers and UI flows; integrate into CI.
- Refactor any remaining large service classes further; apply the Repository pattern consistently.
- Harden environment variable enforcement: fail fast if `${JWT_SECRET}` or `${APP_ADMIN_PASSWORD}` missing.
- Add endpoint‑specific rate‑limit rules using Spring `Bucket4j` or similar.
- Publish the generated OpenAPI spec as a static asset and version it.
- Conduct a compliance review for KYC data handling and update privacy policy accordingly.
