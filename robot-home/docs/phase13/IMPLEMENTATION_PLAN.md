# Phase13 Night 2 Implementation Plan

**Goal:** Continue from the target Phase13 branch and make the enterprise, procurement/CRM, SEO, and MiniApp paths secure and verifiably usable.

**Architecture:** Reuse Phase10 company-member, inquiry, procurement-response, CRM, and behavior-event models. Keep claim/profile changes in the existing company domain with transactional state transitions and MySQL 5.6-compatible nullable unique keys. Serve SEO detail pages as server-generated HTML through the existing Nginx/backend deployment while keeping Vue as the interactive application.

**Tech Stack:** Java 8, Spring Boot 2.7, MyBatis-Plus 3.4.1, MySQL 5.6-compatible SQL, H2 integration tests, Vue 3/Vite, native WeChat MiniApp.

## Global Constraints

- Preserve current uncommitted Phase13 work; inspect it before changing it.
- Do not merge `main`, create Phase14, force-push, reset hard, or clean untracked files.
- Avoid Java 9+ APIs, MySQL 8-only syntax, and unnecessary infrastructure.
- Keep MyBatis-Plus at 3.4.1 as required for this phase.
- Do not access or mutate production data; use the existing isolated H2 test database.
- Do not claim external search-engine crawl verification unless a deployed public host is actually checked.

## Tasks

1. Audit the existing Phase13 diff, established Phase10/12 flows, API docs, and test setup. Record actual completion status in `PROGRESS.md`.
2. Repair claim privacy, unique active claims, retryable rejected/cancelled requests, and compare-and-set review/cancel transitions; add H2 integration tests for the claim lifecycle, duplicate submission, authorization, and concurrent state conflicts.
3. Align company profile-change Java entity and SQL schema, enforce VERIFIED state and active membership for all workspace writes, and test cross-company denial plus review application.
4. Align SEO event entity/schema, prevent anonymous stats exposure and host-header sitemap poisoning, emit valid detail URLs, and add SEO metadata/content/robots/sitemap integration assertions.
5. Add server-rendered HTML for public robot, brand, company, category, ranking, and article routes with escaped metadata/content, canonical URLs, JSON-LD, and valid status codes; connect Nginx routes to this handler.
6. Complete Admin/Web claim review and enterprise workspace UI using the server contracts; finish MiniApp claim submission/history paths and validate route/API contracts.
7. Verify existing procurement response → CRM lead/follow-up lifecycle and tenant isolation; add missing integration assertions without duplicating Phase10 tables or services.
8. Fix verified high-severity cross-cutting authorization issues that block the above flows; run full server and collector tests/packages, Web/Admin builds, MiniApp static/API checks, final code review, migration checks, and `git diff --check`.
9. Update the delivery ledger, commit reviewed Phase13 work, push to `phase13-growth-enterprise-ecosystem` without force, and verify remote SHA and clean working tree.
