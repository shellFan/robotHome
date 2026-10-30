# Video playback and external-link fallback design

Status: **Design for user review; implementation has not started.**

Date: 2026-10-30
Branch: `codex/phase16-content-quality`

## Context

Video records currently expose a URL through the existing `playUrl`, `url`, or `videoUrl` fields. The PC video detail and robot-video dialog pass that value directly to an HTML `<video>` element. The MiniApp detail page does the same with `<video>`. A platform page such as a YouTube watch URL is HTML, not a media stream, so the native player cannot play it.

The MiniApp detail page is maintained in `robot-home/robot-home-miniapp` and copied into the `mall` shell at `modules/robot-home`. The inspected JS and WXML copies match; the mall WXSS additionally imports the shell's shared stylesheet. The behavior must remain consistent in both copies.

## Goals

- Play direct media URLs with the existing native video controls.
- Give PC users a safe way to open a platform video page.
- Give MiniApp users a clear way to copy the source URL and continue on that platform.
- Keep video API fields, database schema, playback counters, likes, favorites, comments, and content records unchanged.
- Avoid proxying, downloading, embedding platform HTML, or changing publication data.

## Considered approaches

1. **Pass every URL to the native player.** This is the current behavior and fails for platform watch pages.
2. **Embed platform pages in a webview or iframe.** Platform support and MiniApp domain rules vary; this also expands the trusted content surface.
3. **Choose behavior by URL type.** Play direct media natively, open ordinary platform links from PC, and copy platform links in MiniApp. This is the selected approach because it works with the existing URL field and has a predictable fallback on both clients.

## URL resolution and classification

1. Resolve the candidate in the existing precedence order: `playUrl`, then `url`, then `videoUrl`.
2. Accept only absolute `http:` or `https:` URLs. Reject malformed values and other schemes such as `javascript:`; do not pass them to a player or navigation API.
3. Classify a valid URL as direct media when its parsed path ends, case-insensitively, in `.mp4`, `.webm`, or `.m3u8`. Query strings and fragments do not affect the extension check, so signed media URLs remain supported.
4. Treat other valid HTTP(S) URLs as external platform pages. The clients do not infer that a URL is official from its hostname; content approval remains responsible for verifying the source before publication.
5. If no valid URL exists, show the video cover and an unavailable state without rendering an empty player or external action.

No API or database fields are added. No remote page is embedded, and no media is downloaded or relayed through the Robot Home server.

## PC behavior

### Video detail

- Direct media renders in the current native `<video>` player with the cover as poster.
- An external platform URL shows the cover and a clear “在原平台观看” action instead of an HTML video player.
- The action opens the stored URL in a new tab using `noopener` and `noreferrer`. Keep a normal safe anchor fallback if scripted popup opening is blocked.
- If native playback reports an error, retain the page and provide the same external action as a fallback.

### Robot video dialog

- Direct media continues to play in the dialog.
- External URLs show the cover, title, and an explicit “在原平台观看” action. Selecting the action opens the URL in a new tab; merely opening the dialog does not launch another page.
- Invalid or missing URLs show an unavailable state.

The existing title, metadata, recommendations, comments, like, favorite, and related-robot actions stay intact.

## MiniApp behavior

- Direct media renders in the native `<video>` component.
- External URLs show the cover and a “复制官方链接” action with a short prompt to open the source platform to watch. Clipboard success displays a confirmation toast. Clipboard failure displays a recoverable message and keeps the page usable.
- If native playback of a direct media URL fails, show a separate “复制视频链接” fallback and explain that the user can try opening the link in a browser. Do not describe a raw media URL as a platform page.
- Invalid or missing URLs show the cover and an unavailable message without a player or copy action.
- Existing like and favorite actions remain unchanged.

Apply the behavior to both `robot-home/robot-home-miniapp/pages/videos/detail.*` and the corresponding `D:/git/fan/mall/modules/robot-home/pages/videos/detail.*` files. Preserve the mall shell's shared WXSS import and do not change shell navigation or global settings.

## Error handling and safety

- URL parsing is guarded; malformed values must not crash page rendering.
- Only HTTP(S) links may reach a player, clipboard, or browser navigation action.
- External pages are never auto-opened and are never loaded inside an iframe or webview.
- A media error changes the visible action state; it does not mutate the record or retry indefinitely.
- If the clipboard API fails, show a concise failure toast and keep the source URL available for a later retry.

## Acceptance criteria

1. `https://cdn.example/video.mp4?token=x`, `.webm`, and `.m3u8` URLs classify as direct media; extension matching is case-insensitive and ignores query/fragment text.
2. A platform watch URL classifies as external and never becomes the `src` of `<video>`.
3. Empty, malformed, `javascript:`, and other non-HTTP(S) values expose no player or outbound action.
4. PC detail and robot-video dialog show an external action for platform URLs; activation opens a new tab with opener isolation.
5. MiniApp detail copies an external URL only after a user tap and confirms success; a direct-media playback error offers a separate copy-video-link fallback.
6. The standalone MiniApp source and mall-integrated copy implement the same classification and user-visible states; the mall shared style import remains present.
7. Existing video API shape and interaction actions remain unchanged. No database migration, production write, video download, or backend change is introduced.
8. Verification covers URL classification, PC production build, MiniApp JS/WXML static checks, and WeChat Developer Tools compilation when that tool is available. Tool/device availability must be reported accurately.

## Scope boundaries

This design covers only video playback and external-link fallback in PC video detail, the PC robot-video dialog, and MiniApp video detail. It does not populate or publish videos, change Admin review, modify video records, or verify production playback. Official-source validation remains a content-review requirement.
