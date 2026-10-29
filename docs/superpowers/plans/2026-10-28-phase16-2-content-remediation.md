# Robot Home Phase16.2 Content Remediation Plan

> **For agentic workers:** Execute this plan in the existing `codex/phase16-content-quality` branch. Preserve the current checkout and all unrelated files. Do not write production data or replace deployed services.

**Goal:** Establish traceable build provenance and prepare evidence-backed, idempotent repair packages for Robot Home production content.

**Architecture:** Keep Server and Collector independently built and deployed. Embed Git/build provenance in both Maven artifacts, generate a deployment manifest at release time, and keep data review separate from mutation by exporting read-only snapshots and producing dry-run plans. Only production inserts/updates after backup and explicit user authorization.

**Tech Stack:** Git/GitHub, Maven 3.9.9, Java 8, Spring Boot 2.7.18, MySQL 5.7, Redis, MinIO, Vue/Vite, WeChat MiniApp, HTTPS APIs.

## Global Constraints

- Push only `origin/codex/phase16-content-quality`; never force push or modify `main`.
- Preserve unrelated tracked and untracked workspace files.
- Never infer the Git SHA of currently deployed JARs; keep it `UNKNOWN` unless metadata proves it.
- No production JAR replacement or production database write without explicit authorization.
- No `TRUNCATE`, `DROP`, Redis flush, edits to other websites' databases, random product images, fabricated community identities, or fake engagement.
- Use read-only database transactions for audits. Data packs must retain source URLs and per-record decisions.
- Run Server and Collector suites with at least 207 and 149 tests respectively, zero failures/errors; rebuild Web/Admin and statically check MiniApp. Record WeChat DevTools/device status separately.

---

### Task 1: Reconcile Git credentials and branch

**Files:** None.

- [x] Confirm remote, credential helper, current local SHA, branch SHA, and `main` SHA.
- [x] Verify the authenticated non-interactive push path and confirm development branch synchronization.
- [ ] Keep per-command HTTP proxy override local to Git commands; do not change global Git/proxy settings.

### Task 2: Build provenance and deployment manifest

**Files:** `robot-home/robot-home-server/pom.xml`, `robot-home/robot-home-collector/pom.xml`, metadata tests in each module, and one release-manifest script under `robot-home/scripts/`.

- [ ] Add red tests requiring `git.properties` and `META-INF/build-info.properties` to expose full commit SHA, branch, build time, and artifact version in both packaged JARs.
- [ ] Add Java 8 compatible Maven metadata generation and Spring Boot build-info to Server and Collector; support detached builds by recording an explicit unknown SHA instead of inventing one.
- [ ] Generate a manifest from already-built JARs with Server/Collector SHA256, Git SHA, branch, build time, deploy time, artifact version, and environment. The script only writes the manifest; it must not replace services or JARs.
- [ ] Run focused tests, full Server/Collector suites, package both JARs, and inspect metadata inside each JAR.

### Task 3: Article export, taxonomy review, and dry-run

**Files:** `docs/phase16.2/data/` export and classification proposal artifacts; no production SQL changes.

- [ ] Export all 37 article IDs, titles, source, summary, category, and status using a read-only transaction.
- [ ] Assign one supported taxonomy label per article only when evidence is sufficient; mark uncertain rows for human review. Do not assign all to “推荐”.
- [ ] Produce an idempotent UPDATE-only dry-run showing before/after category and preserving author, source, and editor-authored content. Do not execute it.
- [ ] Verify category counts equal the proposed categorized rows and compare list/category API behavior without writes.

### Task 4: Product and image audit

**Files:** `docs/phase16.2/data/product-image-audit.csv` and source-check notes.

- [ ] Export each product's ID, name, brand, model, official URL, original image URL, cover URL and status; inspect existing MinIO object references read-only.
- [ ] Test official image candidates with GET, record response/content type and source URL, and avoid relying on HEAD alone.
- [ ] Choose official candidate image or existing standard placeholder; never use random images. Produce per-product action rows and a dry-run plan that preserves manual edits and private bucket permissions.
- [ ] Verify product main image, brand logo and detail image rendering through current HTTP/API paths without changing production data.

### Task 5: Video, tutorial, and community preparation

**Files:** `docs/phase16.2/content/` source-backed video candidates, 12 original tutorial drafts, and official community drafts.

- [ ] Check existing video/tutorial/community routes, moderation state transitions, and playback link handling.
- [ ] Prepare only official or licensed video embeds/deep links with validated source URLs and expected playback behavior; do not fabricate view counts.
- [ ] Write 12 original tutorials with sections, technically checked content, and traceable references; keep them as pending-review drafts.
- [ ] Prepare welcome/rules/discussion community drafts for an official account; do not create user identities or engagement.

### Task 6: Release verification and report

**Files:** `docs/phase16.2/PHASE16_2_RELEASE_AND_DATA_AUDIT.md` and generated artifacts above.

- [ ] Run Server, Collector, Web, Admin, and MiniApp checks; record actual DevTools/device limitations.
- [ ] Recheck live HTTPS pages/APIs read-only and compare current online counts to the starting baseline.
- [ ] Record Git push/local/remote SHA, deployed SHA as UNKNOWN unless proven, backup/dry-run/authorization/write counts, tests, readiness states and all remaining blockers.
- [ ] Do not mark Content Ready or Production Ready while missing content or production repairs remain.
