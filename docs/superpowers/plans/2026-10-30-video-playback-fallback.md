# Video Playback Fallback Implementation Plan

> **For agentic workers:** Execute this plan inline with `superpowers:executing-plans`, one task at a time. Steps use checkbox syntax for tracking.

**Goal:** Make direct media playable and platform-hosted videos usable from PC and MiniApp, including the MiniApp copy embedded in the mall shell.

**Architecture:** Add small client-specific URL classifiers with the same contract (`media`, `external`, `none`), then use that classification in the PC detail page, PC robot-video dialog, and MiniApp detail page. Keep the canonical MiniApp in Robot Home; generate a temporary mall copy and overlay only the three video-detail files into the existing mall worktree, recording the overlay separately from its older base manifest.

**Tech Stack:** Vue 3, Vite 5, native WeChat MiniProgram JavaScript/WXML/WXSS, Node.js built-in test runner, the existing Robot Home to mall sync adapter.

Baseline evidence: canonical MiniApp tests are 3/3 PASS. Mall integration tests are 5/6 PASS; the only current failure hard-codes source SHA `4e9f14efb25fc3dbc03f5d049261260d09bd913c`, while the canonical repo is currently at `2df5600b84ee28daf1220286f379a2b8c1c02ca7`.

## Global Constraints

- Accept only absolute `http:` or `https:` URLs; reject malformed values and other schemes.
- Treat paths ending in `.mp4`, `.webm`, or `.m3u8` (case-insensitive) as direct media; query strings and fragments do not affect classification.
- Open external PC links in a user-initiated new tab with `noopener` and `noreferrer`.
- In MiniApp, copy external platform links only after a user tap; offer a distinct copy-video-link fallback after direct-media playback errors.
- Do not add API or database fields, change playback counters or interactions, proxy or download media, embed remote HTML, publish content, or write production data.
- Keep the current `D:/git/fan/mall` user worktree intact. Do not run the sync adapter against its real `modules/robot-home` target: it deletes that entire target before writing.
- Make mall changes only to the three video-detail page files, the generated-module overlay record, and the existing Robot Home integration test. Do not stage or commit unrelated mall worktree changes.

## File Map

Robot Home repository (`D:/git/fan/AI项目/机器人之家`):

- Create `robot-home/robot-home-miniapp/utils/video-url.js`: CommonJS MiniApp URL classification and existing-field precedence.
- Create `robot-home/robot-home-miniapp/tests/video-url.test.js`: classifier cases and MiniApp page behavior tests.
- Modify `robot-home/robot-home-miniapp/pages/videos/detail.js`, `detail.wxml`, and `detail.wxss`: player, external-link, copy, and error states.
- Create `robot-home/robot-home-web/src/utils/video-url.js`: ES-module classifier with the same contract.
- Create `robot-home/robot-home-web/tests/video-url.test.js`: PC classifier cases.
- Create `robot-home/robot-home-web/tests/video-player-markup.test.js`: guards that platform URLs cannot reach native players and external links remain isolated.
- Modify `robot-home/robot-home-web/src/views/VideoDetail.vue` and `RobotVideos.vue`: media player and safe external-link behavior.
- Update `docs/phase16.3/CONTINUATION_AUDIT_2026-10-30.md` with implementation and verification evidence.

Mall worktree (`D:/git/fan/mall`, already contains extensive user changes):

- Modify only `modules/robot-home/pages/videos/detail.js`, `detail.wxml`, and `detail.wxss` with generated MiniApp output.
- Add a `localOverlays` record to `modules/robot-home/source-manifest.json`, retaining its existing base `sourceCommit` and recording the Robot Home feature commit plus source/target SHA256 values for the three overlaid files.
- Update `scripts/robot_home_integration_test.mjs` so it checks the live canonical source SHA instead of the obsolete pinned `4e9f14e...` value, and asserts the generated detail page distinguishes media from external links.

The mall output will be generated in a temporary directory using a temporary `app.json`. Copying output into the real mall worktree will be limited to the three named page files. The existing mall overlay metadata and all other in-progress work stay local and unstaged.

---

### Task 1: Add MiniApp URL classification with tests

**Files:**

- Create: `robot-home/robot-home-miniapp/tests/video-url.test.js`
- Create: `robot-home/robot-home-miniapp/utils/video-url.js`

**Interface:** `classifyVideoUrl(value)` returns `{ kind: 'media'|'external'|'none', url: string }`. `resolveVideoSource(video)` selects `playUrl`, then `url`, then `videoUrl`, and returns the same classification result.

- [x] **Step 1: Write the failing classifier tests**

Cover signed `.mp4`, uppercase `.WEBM`, `.m3u8#fragment`, an ordinary YouTube watch URL, empty/malformed values, `javascript:`, `ftp:`, and field precedence.

```js
const test = require('node:test')
const assert = require('node:assert/strict')
const { classifyVideoUrl, resolveVideoSource } = require('../utils/video-url')

test('classifies direct media by URL pathname, ignoring query and fragment', () => {
  assert.equal(classifyVideoUrl('https://cdn.example/video.mp4?sig=x').kind, 'media')
  assert.equal(classifyVideoUrl('https://cdn.example/clip.WEBM').kind, 'media')
  assert.equal(classifyVideoUrl('https://cdn.example/live.m3u8#t=4').kind, 'media')
})

test('classifies HTTP platform pages as external and rejects unsafe values', () => {
  assert.equal(classifyVideoUrl('https://www.youtube.com/watch?v=abc').kind, 'external')
  for (const value of ['', 'not a URL', 'javascript:alert(1)', 'ftp://cdn.example/a.mp4']) {
    assert.equal(classifyVideoUrl(value).kind, 'none', value)
  }
})

test('resolves the existing URL field precedence', () => {
  assert.equal(resolveVideoSource({ playUrl: 'https://cdn.example/a.mp4', url: 'https://site.example/watch' }).kind, 'media')
  assert.equal(resolveVideoSource({ url: 'https://site.example/watch', videoUrl: 'https://cdn.example/a.mp4' }).kind, 'external')
})
```

- [x] **Step 2: Run the focused test and confirm it fails**

Run from `D:/git/fan/AI项目/机器人之家/robot-home/robot-home-miniapp`:

```powershell
node --test tests/video-url.test.js
```

Expected: module-not-found failure for `../utils/video-url`.

- [x] **Step 3: Implement the classifier**

Use the platform URL parser inside `try/catch`; accept only HTTP(S), require a hostname, reject username/password credentials, and test the parsed `pathname` with `/\.(?:mp4|webm|m3u8)$/i`. Return the parsed absolute `href` for accepted URLs. Return `{ kind: 'none', url: '' }` for invalid values.

- [x] **Step 4: Run the focused tests**

Run: `node --test tests/video-url.test.js` from the MiniApp directory. Expected: all classifier tests pass.

- [x] **Step 5: Commit the MiniApp classifier and tests**

```powershell
git -C 'D:/git/fan/AI项目/机器人之家' add -- robot-home/robot-home-miniapp/utils/video-url.js robot-home/robot-home-miniapp/tests/video-url.test.js
git -C 'D:/git/fan/AI项目/机器人之家' commit -m "feat(miniapp): classify video playback URLs"
```

### Task 2: Add the matching PC classifier with tests

**Files:**

- Create: `robot-home/robot-home-web/tests/video-url.test.js`
- Create: `robot-home/robot-home-web/src/utils/video-url.js`

**Interface:** ES-module exports `classifyVideoUrl(value)` and `resolveVideoSource(video)` with the same result shape and field precedence as Task 1.

- [x] **Step 1: Write the failing Node tests**

Use `node:test` and `node:assert/strict` to cover direct media with signed query strings, case-insensitive extensions, an external watch page, invalid schemes, malformed input, and field precedence. Keep the cases equal to the MiniApp contract.

- [x] **Step 2: Run the focused test and confirm it fails**

Run from `D:/git/fan/AI项目/机器人之家/robot-home/robot-home-web`:

```powershell
node --test tests/video-url.test.js
```

Expected: module-not-found failure for `../src/utils/video-url.js`.

- [x] **Step 3: Implement the ES-module classifier**

Mirror Task 1's validation and return shape; do not add a dependency or change the backend contract.

- [x] **Step 4: Run the focused tests**

Run: `node --test tests/video-url.test.js` from the PC project. Expected: all cases pass.

- [x] **Step 5: Commit the PC classifier and tests**

```powershell
git -C 'D:/git/fan/AI项目/机器人之家' add -- robot-home/robot-home-web/src/utils/video-url.js robot-home/robot-home-web/tests/video-url.test.js
git -C 'D:/git/fan/AI项目/机器人之家' commit -m "feat(web): classify video playback URLs"
```

### Task 3: Apply classification to both PC players

**Files:**

- Modify: `robot-home/robot-home-web/src/views/VideoDetail.vue`
- Modify: `robot-home/robot-home-web/src/views/RobotVideos.vue`
- Create: `robot-home/robot-home-web/tests/video-player-markup.test.js`

- [x] **Step 1: Write the failing player-markup tests**

The test reads both Vue files and asserts the required boundaries:

```js
assert.match(detailView, /videoSource\\.kind === 'media' && !playbackFailed/)
assert.match(detailView, /target="_blank"[\\s\\S]*rel="noopener noreferrer"/)
assert.match(robotVideosView, /player\\.kind === 'media' && !player\\.error/)
assert.match(robotVideosView, /target="_blank"[\\s\\S]*rel="noopener noreferrer"/)
```

- [x] **Step 2: Run the markup tests and confirm they fail on the existing views**

Run: `node --test tests/video-player-markup.test.js` from the PC project. Before the view changes, both tests failed because the old templates sent any URL directly to `<video>`.

- [x] **Step 3: Update video detail**

Resolve the displayed record through `resolveVideoSource(detail)`. Render `<video>` only when `kind === 'media'` and playback has not failed. For `kind === 'external'`, render the cover and an anchor using `target="_blank"` and `rel="noopener noreferrer"`. For `kind === 'none'`, show the cover and unavailable text. On a media `error`, retain the source and show a user-activated link fallback. Reset the failure state whenever a new route record loads.

- [x] **Step 4: Update the robot-video dialog**

Store the selected video's classified `{kind, url}` and cover in dialog state. Direct media remains in the dialog's native player. External pages show the cover/title and a safe new-tab anchor; opening the dialog itself never opens a new tab. Invalid values show the existing unavailable state. A player error exposes a link fallback.

- [x] **Step 5: Build PC and run focused tests**

Run from `robot-home/robot-home-web`:

```powershell
node --test tests/video-url.test.js tests/video-player-markup.test.js
node node_modules/vite/bin/vite.js build
```

Result: 6/6 PC tests pass; Vite production build exits 0.

- [x] **Step 6: Commit PC playback handling**

```powershell
git -C 'D:/git/fan/AI项目/机器人之家' add -- robot-home/robot-home-web/src/views/VideoDetail.vue robot-home/robot-home-web/src/views/RobotVideos.vue
git -C 'D:/git/fan/AI项目/机器人之家' commit -m "fix(web): route platform videos to safe external links"
```

### Task 4: Apply classification and clipboard fallback to canonical MiniApp

**Files:**

- Modify: `robot-home/robot-home-miniapp/pages/videos/detail.js`
- Modify: `robot-home/robot-home-miniapp/pages/videos/detail.wxml`
- Modify: `robot-home/robot-home-miniapp/pages/videos/detail.wxss`
- Modify: `robot-home/robot-home-miniapp/tests/video-url.test.js`

- [x] **Step 1: Add page tests before implementation**

Load the page definition in a Node `vm` context with mocked `Page`, `videoApi`, `wx`, and `setData`. Assert that platform URLs never populate the `<video>` binding, direct URLs do, playback errors set the fallback state, and clipboard success/failure produce the correct toast without mutating the URL.

```js
assert.equal(page.data.video.playbackKind, 'external')
assert.equal(page.data.video.playUrl, '')
assert.equal(page.data.video.externalUrl, platformUrl)
assert.equal(page.data.video.showPlayer, false)
assert.equal(page.data.video.playbackKind, 'media')
assert.equal(page.data.video.showCopyOnFailure, true)
assert.equal(lastToast.title, '复制失败，请稍后重试')
```

- [x] **Step 2: Run the focused tests and confirm the page cases fail**

Run: `node --test tests/video-url.test.js` from the MiniApp directory. Expected: classifier tests pass and the new page behavior assertions fail against the old page definition.

- [x] **Step 3: Implement MiniApp page state and handlers**

On load, classify the selected URL and store separate `playUrl`, `externalUrl`, `playbackKind`, `showPlayer`, `showExternalLink`, `showCopyOnFailure`, and `showUnavailable` values. Bind `binderror` on `<video>` to set the fallback state. The external-link button calls `wx.setClipboardData` only after a tap and reports success/failure with `wx.showToast`. A failed direct stream uses a separate “复制视频链接” action. Preserve likes, favorites, summary, poster, and view text.

- [x] **Step 4: Update WXML and WXSS**

Render `<video>` only for direct media before an error. Use the cover for external, failed, or invalid states. Render “复制官方链接” plus a source-platform hint only for external URLs; render “复制视频链接” with a browser hint after direct playback failure; render an unavailable message for `none`. Add only local layout styles and retain the page's existing shared stylesheet convention.

- [x] **Step 5: Run canonical MiniApp tests and syntax checks**

Run from `robot-home/robot-home-miniapp`:

```powershell
node --test tests/*.test.js
node --check pages/videos/detail.js
```

Result: 10/10 MiniApp tests pass and syntax check exits 0.

- [x] **Step 6: Commit canonical MiniApp behavior**

```powershell
git -C 'D:/git/AI项目/机器人之家' add -- robot-home/robot-home-miniapp/pages/videos/detail.js robot-home/robot-home-miniapp/pages/videos/detail.wxml robot-home/robot-home-miniapp/pages/videos/detail.wxss robot-home/robot-home-miniapp/tests/video-url.test.js
git -C 'D:/git/AI项目/机器人之家' commit -m "fix(miniapp): add platform video copy fallback"
```

### Task 5: Update the mall-generated copy without replacing the active worktree

**Files:**

- Modify: `D:/git/fan/mall/scripts/robot_home_integration_test.mjs`
- Modify: `D:/git/fan/mall/modules/robot-home/pages/videos/detail.js`
- Modify: `D:/git/fan/mall/modules/robot-home/pages/videos/detail.wxml`
- Modify: `D:/git/fan/mall/modules/robot-home/pages/videos/detail.wxss`
- Modify: `D:/git/fan/mall/modules/robot-home/source-manifest.json`

- [ ] **Step 1: Make the integration test follow the canonical source HEAD**

Import `execFileSync` from `node:child_process` and replace the fixed expected source SHA with this live value:

```js
const currentSourceCommit = execFileSync(
  "git",
  ["-C", sourceRoot, "rev-parse", "HEAD"],
  { encoding: "utf8" },
).trim();
assert.equal(manifest.sourceCommit, currentSourceCommit);
```

Add a test against the generated temporary detail page that verifies the page imports `resolveVideoSource`, renders the player only for `playbackKind === 'media'`, and has an external-link copy action for `playbackKind === 'external'`.

- [ ] **Step 2: Generate a temporary mall module**

Run the existing adapter with a unique temporary root and a temporary app manifest. Never pass the real mall module directory because `syncRobotHome` removes its target recursively before generating.

```powershell
$videoSyncRoot = Join-Path ([System.IO.Path]::GetTempPath()) ('rh-video-sync-' + [guid]::NewGuid().ToString('N'))
$videoSyncTarget = Join-Path $videoSyncRoot 'modules/robot-home'
New-Item -ItemType Directory -Path (Join-Path $videoSyncRoot 'modules') -Force | Out-Null
'{"pages":[],"subpackages":[]}' | Set-Content -LiteralPath (Join-Path $videoSyncRoot 'app.json') -Encoding utf8
node 'D:/git/fan/mall/scripts/sync_robot_home.mjs' `
  --source 'D:/git/fan/AI项目/机器人之家/robot-home/robot-home-miniapp' `
  --target $videoSyncTarget `
  --app-json (Join-Path $videoSyncRoot 'app.json') `
  --api-base-url 'https://jqr.21zuo.com/api'
```

- [ ] **Step 3: Copy only the three generated video-detail files**

After checking the temporary outputs, copy only `pages/videos/detail.js`, `detail.wxml`, and `detail.wxss` into `D:/git/fan/mall/modules/robot-home/pages/videos/`. Preserve all other module files, `app.json`, mall navigation, and unrelated in-progress changes.

- [ ] **Step 4: Record the overlay without falsifying the base manifest**

Keep the existing base `sourceCommit` (`4e9f14efb25fc3dbc03f5d049261260d09bd913c`). Add or replace a `localOverlays` entry named `video-playback-fallback` containing the committed Robot Home SHA and source/target SHA256 values for each of the three files. Do not rewrite base source file hashes or claim the full module was regenerated. Preserve other overlay entries. Use this Node command after the page copies and Robot Home feature commit exist:

```powershell
@'
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

const sourceRoot = "D:/git/fan/AI项目/机器人之家/robot-home/robot-home-miniapp";
const mallRoot = "D:/git/fan/mall";
const manifestPath = path.join(mallRoot, "modules/robot-home/source-manifest.json");
const paths = ["pages/videos/detail.js", "pages/videos/detail.wxml", "pages/videos/detail.wxss"];
const hash = (file) => crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
const manifest = JSON.parse(fs.readFileSync(manifestPath, "utf8"));
const overlays = Array.isArray(manifest.localOverlays) ? manifest.localOverlays : [];
const files = paths.map((relative) => ({
  path: relative,
  sourceSha256: hash(path.join(sourceRoot, relative)),
  targetSha256: hash(path.join(mallRoot, "modules/robot-home", relative)),
}));
manifest.localOverlays = [
  ...overlays.filter((entry) => entry.name !== "video-playback-fallback"),
  {
    name: "video-playback-fallback",
    sourceCommit: execFileSync("git", ["-C", sourceRoot, "rev-parse", "HEAD"], { encoding: "utf8" }).trim(),
    files,
  },
];
fs.writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);
'@ | node --input-type=module -
```

- [ ] **Step 5: Run mall integration verification**

Run from `D:/git/fan/mall`:

```powershell
node --test scripts/robot_home_integration_test.mjs
node --check modules/robot-home/pages/videos/detail.js
```

Expected: the baseline source-commit assertion no longer fails, all integration cases pass, and generated/copied detail code parses. Run `scripts/tests/Test-RobotHomeWxmlBindings.ps1` and confirm the mall detail WXML contains no JavaScript method calls.

### Task 6: Full local verification, final report, and push

- [ ] **Step 1: Run MiniApp and PC verification**

Run the MiniApp suite, both URL classifier suites, the Web production build, and the mall integration test. Confirm exact pass/fail counts and preserve output logs under `docs/phase16.3/logs/` (the directory is ignored by Git).

- [ ] **Step 2: Verify all generated copies and working-tree boundaries**

Compare canonical and generated target SHA values recorded in `localOverlays`. Confirm only the three intended `modules/robot-home/pages/videos/detail.*` files and `source-manifest.json` changed in the mall workspace. Do not stage any mall files because that workspace already has extensive user-owned modifications.

- [ ] **Step 3: Update the Phase16.3 report and inspect the Robot Home diff**

Record tests, builds, current branch SHA, overlay SHA values, and MiniApp Developer Tools/device status in `docs/phase16.3/CONTINUATION_AUDIT_2026-10-30.md`. Run `git diff --check` and review every staged path before commit.

- [ ] **Step 4: Commit and push only to the authorized development branch**

Commit the Robot Home code/report changes on `codex/phase16-content-quality`. Confirm `origin/codex/phase16-content-quality` is the expected parent before a normal push; verify `origin/main` is unchanged afterward. Do not force-push or deploy.

## Self-Review

- Spec URL resolution, direct-media extensions, invalid values, and precedence are covered by Tasks 1–2.
- PC detail, robot-video dialog, safe new-tab behavior, and media-error fallback are covered by Task 3.
- MiniApp external copy, direct playback, playback-error copy, invalid state, and preserved user actions are covered by Task 4.
- The mall shell copy, base manifest truthfulness, and generated integration test are covered by Task 5; the destructive full-target sync path is expressly excluded.
- Build/test counts, hashes, commit/push evidence, and device-test limits are recorded by Task 6.
- No API/database schema, production data, deployment, remote content, or infrastructure changes are included.
