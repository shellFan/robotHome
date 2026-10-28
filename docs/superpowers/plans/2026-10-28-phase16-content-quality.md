# Phase16 Content Quality Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reconcile code and runtime versions, finish and verify the existing changes, audit production content read-only, and publish an evidence-based Phase16 report.

**Architecture:** Preserve all current workspace edits, compare local and fetched Git refs, and collect production evidence through read-only SSH/HTTP/SQL. Fix verified code defects with focused regression coverage, run each component's available verification, then commit and push only a development branch.

**Tech Stack:** Git, PowerShell/OpenSSH or Paramiko, Java 8/Spring Boot/Maven, Vue/Vite/npm, WeChat MiniApp, MySQL/Redis, HTTPS APIs.

## Global Constraints

- Never run `reset`, `clean`, force push, destructive SQL, or infrastructure deployment.
- Keep production database inspection read-only; request confirmation before any production write or service replacement.
- Mark runtime Git SHA `UNKNOWN` unless build metadata or deployment evidence proves the mapping.
- Never fabricate community users, content, or interactions; use traceable real sources for any proposed content.
- Preserve existing work and push only a development branch, never overwrite `main`.

---

### Task 1: Version and workspace reconciliation

**Files:** No source edits; record evidence in the Phase16 report.

- [ ] Record current branch, HEAD, fetched `origin/main`, ahead/behind, and all tracked/untracked changes.
- [ ] Read-only inspect systemd, deployment JAR hashes and build metadata, frontend version markers, deployment logs, and HTTPS/API responses.
- [ ] Mark server SHA `UNKNOWN` if evidence cannot establish it.

### Task 2: Review and finish existing code changes

**Files:** Existing modified Server, Collector, Admin, and runtime verification files plus focused tests.

- [ ] Review every existing diff and untracked test; trace collector transaction, body extraction, category aggregation, images, and database compatibility.
- [ ] For each confirmed defect, add a regression test, observe its failure, make a minimal fix, and rerun it.
- [ ] Run Server, Collector, Web, Admin, and MiniApp checks requested by the user; record commands and outcomes.

### Task 3: Read-only production content audit

**Files:** Read-only evidence and report only.

- [ ] Obtain actual counts and moderation states for articles, products, brands, videos, tutorials, community, and images.
- [ ] Compare representative API responses, frontend routes, and image HTTP status; determine causes for missing categories, cookie pages, product/article misclassification, and broken images.
- [ ] Do not write production data until backup, dry-run, and explicit authorization are available.

### Task 4: Development branch and Phase16 report

**Files:** New `docs/phase16/PHASE16_RELEASE_ACCEPTANCE_REPORT.md` and reviewed code fixes.

- [ ] Create/switch to `codex/phase16-content-quality` without discarding the workspace.
- [ ] Stage only intended project files, commit after verification, and push that branch without modifying `main`.
- [ ] Report exact local/remote/server versions, test evidence, production counts and HTTP checks, unresolved blocks, and the five readiness statuses.
