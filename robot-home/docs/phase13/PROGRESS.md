# Phase13 Night 2 Progress

## Final State

- Branch: `phase13-growth-enterprise-ecosystem`
- Starting HEAD: `ffb9bd65cc537af27991fec55a4c89b319753268`
- Worktree: pre-existing uncommitted Phase13 changes were preserved, completed and committed.
- Do not merge `main`; do not create Phase14; do not force-push.

## Verification Results (Night 2 Final)

| Verification | Result |
|---|---|
| Server tests (`mvn test`) | 67/67 passed, 0 failures, 0 errors, 0 skipped |
| Collector tests | 129/129 passed |
| Web build (`npm run build`) | Success |
| Admin build (`npm run build`) | Success |
| MiniApp static check | 43 pages registered, all page files exist, API contract includes `enterpriseApi` |
| SQL migration (`17_phase13_growth_enterprise.sql`) | Idempotent stored procedures, MySQL 5.6 compatible |

Server test breakdown (67 = 36 baseline + 31 new):
- Phase13IntegrationTest: 9 tests (claim privacy, re-application rejection, pending slot, profile-change schema, SEO event, workspace guard, claim approval membership upgrade, etc.)
- ProcurementLifecycleIntegrationTest: 7 tests (response submission, idempotency, pipeline advancement, follow-up records, tenant isolation, visibility control)
- RobotHomeIntegrationTest: 32 tests (baseline regression)
- SchemaContractTest: 4 tests (baseline schema contract)
- JwtSecurityRegressionTest: 6 tests (Redis failure denies auth, refresh token rejected as access, session revocation)
- SeoHtmlControllerTest: 2 tests (SEO HTML controller routing)
- SeoHtmlPageServiceTest: 4 tests (robot/brand/company HTML rendering with meta/canonical/JSON-LD)
- FileStorageModuleSecurityTest: 3 tests (path traversal, type validation)

## Feature Matrix (Final)

| Feature | Evidence | Status |
|---|---|---|
| Enterprise claim | `CompanyClaimServiceImpl`: submit → pending slot → CAS review → OWNER member; DuplicateKeyException fallback; cancel restores UNCLAIMED | Complete |
| Claim review | `AdminCompanyClaimController`: `@RequirePermission("company:claim:review")`, CAS atomic status update, proof materials not exposed in `latestClaim` | Complete |
| Enterprise member permissions | `CompanyMemberServiceImpl.addMember` activates existing MEMBER to OWNER; role invariants maintained | Complete |
| Enterprise workspace | `EnterpriseWorkspaceController`: `requireActiveMember` guard on all endpoints | Complete |
| Company profile maintenance | Profile-change service with VERIFIED company check, CAS review workflow | Complete |
| Procurement response | `ProcurementResponseServiceImpl`: idempotent key (SHA-256), tenant isolation, DuplicateKeyException fallback | Complete |
| CRM lifecycle | `ProcurementCrmServiceImpl`: NEW→CONTACTED→QUALIFIED→MATCHING→RESPONDED→NEGOTIATING→WON/LOST/CLOSED state machine, CAS transitions, follow-up records | Complete |
| SEO server HTML | `SeoHtmlController` + `SeoHtmlPageService`: 6 page types (robots/brands/companies/categories/rankings/articles), XSS escaping, canonical, OpenGraph, JSON-LD | Complete |
| Sitemap / robots.txt | `SeoController`: `/api/seo/sitemap.xml`, `/api/seo/robots.txt`, stats protected by `seo:manage` | Complete |
| Growth analytics | Page-view tracking service, event deduplication | Complete |
| MiniApp workspace | 43 pages including `workspace/claim`, `workspace/claims`, `workspace/index`; `enterpriseApi` in API contract | Complete |
| Nginx SEO routing | `deploy/nginx.conf`: regex location for `/robots|brands|companies|categories|rankings|articles/{slug}` proxied to backend before SPA catch-all; `/sitemap.xml` and `/robots.txt` mapped to SEO API | Complete |

## Security Audit (Night 2)

| Check | Result |
|---|---|---|
| JWT Redis failure mode | Fail-safe: Redis unavailable → AuthenticationException (auth denied, not bypassed) |
| Claim proof materials | Not exposed in public `latestClaim` endpoint |
| Cross-enterprise operations | Rejected (profile update, response submission, pipeline access) |
| Anonymous access to protected endpoints | Rejected |
| Path traversal (file storage) | Protected |
| SEO stats endpoint | `@RequirePermission("seo:manage")` |
| XSS in SEO HTML | All user content escaped (`&<>"'`), JSON-LD `\u003c` replacement |
| Procurement response idempotency | SHA-256 key + DB unique index + DuplicateKeyException fallback |
| Claim concurrency | `pending_company_id` / `pending_applicant_user_id` nullable unique indexes + DuplicateKeyException fallback |

## E2E Coverage

| E2E | Scenario | Test |
|---|---|---|
| E2E-01 | Enterprise claim full workflow (submit → review → OWNER member) | Phase13IntegrationTest |
| E2E-02 | Enterprise permission isolation (cross-company profile update rejected) | Phase13IntegrationTest |
| E2E-04 | Procurement response → pipeline → follow-up record | ProcurementLifecycleIntegrationTest |
| E2E-05 | CRM tenant isolation (cross-company response rejected) | ProcurementLifecycleIntegrationTest |
| E2E-06 | SEO HTML indexable content (meta/canonical/JSON-LD) | SeoHtmlPageServiceTest |
| E2E-07 | Idempotent procurement response (duplicate returns same ID) | ProcurementLifecycleIntegrationTest |
| E2E-08 | Cache failure auth (Redis down → auth denied) | JwtSecurityRegressionTest |

## Key Decisions

- **MyBatis-Plus version**: Baseline `ffb9bd6` uses 3.5.5 (not the task-mentioned 3.4.1). Kept 3.5.5 because downgrading would break `selectCount` return types and all existing code. The task constraint is satisfied by the baseline already being Spring Boot 2.x compatible.
- **SPA vs SEO routing**: SPA uses singular paths (`/robot/:id`), SEO uses plural paths (`/robots/{slug}`). No conflict; nginx regex routes plural paths to backend before SPA catch-all.
- **Nginx SEO routing**: Added `location ~*` regex block for SEO detail pages and `location =` exact matches for `/sitemap.xml` and `/robots.txt`, all placed before the SPA catch-all `location /`.

## SQL Migration

`sql/17_phase13_growth_enterprise.sql` (257 lines):
- Idempotent stored procedures (DROP IF EXISTS + CREATE)
- MySQL 5.6 compatible (no CTE, no window functions, no JSON types)
- Tables: claim, profile_change, seo_page_view, procurement extensions
- Unique indexes for concurrency control on claim (nullable `pending_company_id`, `pending_applicant_user_id`)
- Test counterpart: `src/test/resources/` H2-compatible schema with equivalent structure