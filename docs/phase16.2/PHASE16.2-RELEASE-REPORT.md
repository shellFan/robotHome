# Robot Home Phase16.2 Release and Content Audit

Audit date: 2026-10-29 (Asia/Shanghai)
Branch: `codex/phase16-content-quality`
Production writes: **none**. Production JARs/services were not changed.

## Release status

| Item | Result | Evidence |
|---|---|---|
| Git push | PASS | Commits pushed over HTTPS to `origin/codex/phase16-content-quality`; remote `main` was not changed. Git Credential Manager lists the authorized `shellFan` account. No token was read or stored. |
| Server tests | PASS, 208 tests, 0 failures/errors | Fresh Maven/Surefire run, Java 8.0.511 and available Maven 3.3.9. |
| Collector tests | PASS, 151 tests, 0 failures/errors | Fresh Maven/Surefire run, Java 8.0.511 and available Maven 3.3.9. |
| Web | PASS | `pnpm run build`, Vite production build. |
| Admin | PASS | Direct existing Vite production build passed. `pnpm run build` itself was blocked by pnpm's ignored-build-script policy for `es5-ext`, `esbuild`, and `vue-demi`; no dependency scripts were approved or installed. |
| MiniApp | Static checks PASS; WeChat compiler/device UNKNOWN | Page JS syntax and regression checks pass. WeChat Developer Tools was not found on this machine; no simulator or physical-device result is claimed. |
| Local JAR provenance | PASS, verification-only | Clean Server and Collector JARs were rebuilt from `6d759543159f784dea022b0866b398e7f1098c24`. The manifest records each SHA256, Git SHA/branch, build time, version `1.0.0`, and `NOT_DEPLOYED`; see `deployment-manifest.local.json`. |
| Production deployment | NOT DONE | Existing production JAR Git SHA remains **UNKNOWN**. No JAR replacement or service restart was performed. |

## Online data, read-only HTTP verification

All listed GET endpoints returned HTTP 200 on `https://jqr.21zuo.com` during this audit. Counts were read from complete first-page responses (`pageSize=100`):

The site root, `/admin/`, `/robots`, and `/articles` also returned HTTP 200 HTML responses. This verifies HTTP delivery only; no authenticated admin workflow or browser-rendered interaction was exercised.

| Content | Before | After | Additional evidence |
|---|---:|---:|---|
| News | 37 | 37 | 0 categorized; all 12 category counts returned as 0. |
| Products/robots | 56 | 56 | 0 with cover/main image in API response; DB audit also found zero `robot_image` records. |
| Brands | 47 | 47 | 12 logos point to `picsum.photos`; 35 have no logo. |
| Videos | 0 | 0 | No production records. |
| Tutorials | 0 | 0 | No production records. |
| Community posts | 0 | 0 | No production records. |
| Community circles | 9 | 9 | Circle list endpoint returned all 9. |

Read-only GET paths checked: `/api/articles/categories`, `/api/articles`, `/api/robots`, `/api/brands`, `/api/videos`, `/api/tutorials`, `/api/community/circles`, `/api/community/posts`. No POST/PUT/PATCH/DELETE request was sent. The earlier database audit used a read-only transaction and rollback; its exports and row-level audit files are in `docs/phase16.2/data/`.

## Data preparation and root causes

- The 37-row news dry-run proposes category `融资` only for the one article with a reliable classification signal. The other 36 are marked for manual review or content-type/status review. Recommendation flags are not mass-enabled. SQL is in `data/news-category-update.dry-run.sql`; it was **not executed**.
- News source fields are empty and some extracted text is Cookie/site-template material. The generic crawler had no include/exclude rules or content selectors, and had accepted non-article pages. Collector code now skips unclassified pages and no longer defaults generic pages to articles; regression coverage was added.
- The 56-product image audit found no stored covers, galleries, or detail-image tags. Three official product-page image candidates were identified, but usage permission is not confirmed. They were not imported. Product URL checks included 20 HTTP 200, 27 HTTP 404, 4 HTTP 500, 1 HTTP 403, and 4 request failures; failed URLs are not treated as verified sources.
- Ten video candidates are recorded in `content/video-candidates.csv`; the target list is official-source oriented, but legal embed/playback could not be verified in this environment. No videos were written or published.
- Twelve original tutorial drafts and three community drafts are prepared under `content/`. They have not been created in production or passed the admin approval workflow. No fake users or engagement were created.
- No production backup was made because no production mutation was authorized or performed. The CSV extracts are audit exports, not a restorable database backup.

## MiniApp WXML correction

The standalone MiniApp's plans page called `.toFixed()` inside a WXML binding, which causes the reported template parse error. The page now formats each plan's price in JS as `priceText`; WXML renders only `{{item.priceText}}`. The mall-integrated copy already used this pattern. Both copies pass the static regression check and the standalone JS passes `node --check`. WeChat compiler and device verification remain outstanding.

## Data change ledger

```yaml
data_backup: NOT_CREATED (no authorized mutation)
dry_run: news classification CSV and idempotent SQL prepared; product audit prepared
production_write_authorization: NOT_GRANTED
actual_inserts: 0
actual_updates: 0
actual_deletes: 0
```

## Readiness

```yaml
Code Ready: YES (tests/builds passed and development branch pushed)
Deployment Verified: NO (production commit SHA remains UNKNOWN; no deployment was performed)
Runtime Ready: PARTIAL (public APIs respond, but no runtime deployment/version proof)
Content Ready: NO (content issues remain; drafts are unpublished)
Production Ready: NO
```

## Final ledger

```yaml
Git Push: PASS (origin/codex/phase16-content-quality)
Artifact Build Source SHA: 6d759543159f784dea022b0866b398e7f1098c24
Production Server Deployed SHA: UNKNOWN
Production Collector Deployed SHA: UNKNOWN
News Before/After: 37/37
Categorized News: 0
Products Before/After: 56/56
Products With Covers: 0
Videos: 0 online; 10 candidates prepared, none published
Tutorials: 0 online; 12 drafts prepared, none published
Community Posts: 0 online; 3 official drafts prepared, none published
Data Backup: NOT CREATED (no production mutation)
Dry Run: news classification SQL/CSV and product image audit prepared
Production Write Authorization: NOT GRANTED
Actual Inserts: 0
Actual Updates: 0
Actual Deletes: 0
Server Tests: 208 passed, 0 failures, 0 errors
Collector Tests: 151 passed, 0 failures, 0 errors
Web: PASS
Admin: PASS (direct Vite build; pnpm build blocked by ignored build-script policy)
MiniApp: Static PASS; WeChat Developer Tools/device UNKNOWN
```

## Remaining blockers

1. Push the committed development branch; no `main` changes or force push are planned.
2. Production deployment and production database writes require separate explicit authorization; the existing production JAR must first be backed up and its deployment recorded.
3. Approve and execute the news dry-run only after row-level human review; prepare a restorable production backup first.
4. Obtain permissible product-image sources/licensing; do not use random placeholders as product photos.
5. Verify video embed/deep-link behavior, send tutorial/community drafts through the existing review flow, then recheck HTTP/UI counts.
6. Run the MiniApp in WeChat Developer Tools and on a physical device.
