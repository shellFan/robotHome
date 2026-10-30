# Phase16.3 继续验收记录

验收时间：2026-10-30 13:17（Asia/Shanghai）。本报告记录 12:12 最终快照后的继续工作；`FINAL_RELEASE_GATE.md` 已标注为历史快照。

## Git 与工作区

- 仓库：机器人之家，分支 `codex/phase16-content-quality`。
- 开始时本地 HEAD、`origin/main` 和 `origin/codex/phase16-content-quality` 均为 `2c75979a8d93729293f20e856ffb4a8c97cf8d62`。
- 实现与审计提交 `1bd2a0e1232643fa1996575c90b7c8cace79cf5d` 已成功推送到 `origin/codex/phase16-content-quality`；推送后核验远端开发分支与该提交一致，`origin/main` 仍为 `2c75979a8d93729293f20e856ffb4a8c97cf8d62`。本报告的推送状态只在开发分支记录。
- 本轮变更包括资讯审计修订、产品/视频/教程报告补充、教程草稿修订、分类 dry-run v3、仅供本地使用的 JSON staging 预演器及其测试。
- 代码及报告已完成审查和测试。未对 `main` 执行 push、合并或改写；未使用 reset、clean 或 force push。
- `docs/phase16.3/logs/` 下保存本轮 Server、Collector、Web、Admin、MiniApp 与 importer 的测试/构建日志。

## 生产环境与线上数据

- 用户暂时无法通过可信控制台核对 SSH 主机指纹 `SHA256:niEFrPybZrWYn+fCBpwASJFL7M/hwXpqSGalEpWHRew`；因此本轮未建立 SSH 连接、未检查 systemd/JAR/数据库/Redis/MinIO，也没有部署。
- 2026-10-30 13:12 对公开列表、分类、搜索 API 做了只读 GET 复核，并重新检查 8 个 PC SPA 路由。数据为：资讯 37、分类 12、已分类资讯 0、产品 56、带封面的产品 0、品牌 47、已核实 Logo 0、视频 0、教程 0、社区帖子 0、圈子 9。公开 API 和 8 个 SPA 路由均 HTTP 200；12/12 分类筛选业务成功且总数为 0。未请求资讯详情接口。详情请求的既有计数副作用见下文。
- 资讯分类 dry-run v3 覆盖 37 条，逐条导出 ID、标题、来源、作者捕获状态、正文摘要、当前分类、发布状态、建议类别、内容类型建议、推荐建议、判定和依据。15 条为产品页，21 条为企业/服务/政策/入口类页面，1 条为历史企业新闻候选；全部要求人工复核。没有生产写入。
- 产品封面审计：56 个产品中，21 个官方 URL 为 HTTP 200、25 个为 404、1 个为 403、9 个无可用状态/请求失败；没有已核实来源和使用许可的候选图。13:12 API 复核中，产品封面字段非空数为 0；47 个品牌有 12 个非空 Logo 字段，但本轮没有确认其官方性、时效或可访问性，已核实 Logo 仍为 0。产品图、品牌 Logo 和 MinIO 对象均未写入或调整。
- 视频候选 10 条，均未验证播放器链路或发布；教程草稿 12 篇，社区官方草稿 3 篇。均未写入线上。圈子仍为 9 个；未伪造用户或互动。
- 生产备份、恢复演练、生产 dry-run 核验、生产写入授权、插入、更新、下架、删除、服务替换均未执行。

### 详情请求的计数副作用

此前读取资讯 ID 1–37 的详情两遍，共 74 次 GET。服务实现会在详情事务里将 `article.view_count` 加一。第二遍明确成功 37/37；第一遍输出被截断，成功数未知。因此至少 37 次、最多 74 次浏览计数更新可能已提交，最终持久化数量无法核验。没有手动扣减或覆盖计数；本轮没有再次请求资讯详情 API。

## 代码与本地 staging

- `robot-home/scripts/content/import-content.mjs` 只读取 JSON/教程 Markdown，输出带源文件 SHA256 的 dry-run；`--apply` 只写报告目录内的本地 JSON staging 文件，没有数据库驱动或网络写入能力。
- 修复了来源键规范化、原型属性来源键、教程摘要提取、符号链接越界、apply 失败审计和回滚重试问题。apply 会先保存包含完整计划的运行报告，再一次性原子写入 staging state；若最终报告更新失败，初始报告与 state 操作日志可用于核对。报告只统计实际写入批次。对应回归测试先失败、修复后通过。
- 12 篇教程 staging 均为待审核状态 `status=0`。本地 apply 前 dry-run 检查了 12 条 `UPDATE_STAGING`，分 3 批更新；随后重跑 dry-run 得到 12 条 `SKIP_UNCHANGED`，操作日志保持 26 条。它不代表教程已进入线上数据库或已发布。
- 资讯、图片、视频、教程和社区内容均未向生产环境写入。
- **Minor 限制：** 若 staging state 原子写入成功、最终报告状态重写失败，报告会保留 `APPLYING`。报告中的 batch ID 与 state 操作日志可供人工核对；后续命令目前不会自动修正这类旧报告。

## 测试与构建

| 范围 | 结果 | 日志 |
|---|---|---|
| Server | 208/208，0 failures，0 errors | `logs/server-tests-continuation-2026-10-30.log` |
| Collector | 174/174，0 failures，0 errors | `logs/collector-tests-continuation-2026-10-30.log` |
| Web | Vite production build PASS | `logs/web-build-continuation-2026-10-30.log` |
| Admin | Vite production build PASS | `logs/admin-build-continuation-2026-10-30.log` |
| Admin image tests | 2/2 PASS | `logs/admin-image-tests-continuation-2026-10-30.log` |
| MiniApp | JS 54、JSON 50、声明页面 46、缺失页面文件 0 | `logs/miniapp-static-continuation-2026-10-30.log`；小程序开发者工具/真机未运行 |
| MiniApp tests | 3/3 PASS | `logs/miniapp-tests-continuation-2026-10-30.log` |
| Local content staging importer | 11/11 PASS | `logs/importer-tests-continuation-2026-10-30.log` |

Web/Admin 的 package script 均是 `vite build`。本机 Node 运行时没有 npm 命令；测试通过对应项目已安装的 Vite CLI 执行了相同构建任务。

以上原始 `.log` 文件受仓库 `.gitignore` 规则排除，保存在当前工作区供复核；本次提交包含本报告中的测试结果摘要，不包含原始日志文件。

## 状态

- **Code Ready：** YES（Server/Collector 测试、Web/Admin 构建、MiniApp 静态检查和相关 Node 测试通过）。
- **Deployment Verified：** NO（可信 SSH 主机身份未核验；生产 JAR SHA、部署 SHA、systemd 状态均 UNKNOWN）。
- **Runtime Ready：** PARTIAL（此前公开 HTTPS 路由/API 可响应；本轮未刷新线上运行检查）。
- **Content Ready：** NO（资讯分类 0、产品无已核实封面、视频/教程/社区帖子均为 0）。
- **Production Ready：** NO。
- **MiniApp 真机验收：** NOT RUN。

PC/API 只读 HTTP 明细记录于当前工作区 `logs/public-http-continuation-2026-10-30.log`。共 31 次 GET；未执行 POST/PUT/DELETE、Admin 登录或资讯详情请求。本轮没有生产内容变更。

## 阻塞项

1. 需要从可信宝塔/云控制台核验 SSH 主机指纹后，才能开展获准的生产只读巡检。
2. 需要完成数据库备份与隔离恢复演练，并审阅逐条 dry-run 后，才能请求具体生产数据写入授权。
3. 图片需确认厂商来源和使用许可；视频需确认微信/PC 播放方式；教程需技术审校并经 Admin 审核；社区草稿需官方账号审核。
4. 微信开发者工具、模拟器、真机、Admin 登录审核及 MySQL 5.6 运行时兼容尚未验收。

## 13:28 继续工作补记

### 当前代码和分支

- 本地分支和 `origin/codex/phase16-content-quality` 在本次修改前均为 `f1a871f368c9a598792a7f897c5f76a3aec0ebe7`；`origin/main` 为 `2c75979a8d93729293f20e856ffb4a8c97cf8d62`。分支未切换，未改写历史。
- 本地未提交修改仅包括 `robot-home/scripts/db/restore.sh`、`robot-home/scripts/db/verify-backup.sh`、`robot-home/scripts/tests/Test-BackupCredentialGuard.ps1` 和新测试 `robot-home/scripts/tests/test-verify-backup.sh`。前两处补全恢复前备份中的 routines/triggers/events，并修复大型 gzip SQL 上 `grep -q` 与 `pipefail` 引发的误报；新测试覆盖有效大备份及必需表缺失。
- 当前新鲜验证：验证器回归 2/2 PASS；gzip 失败关闭回归 PASS；凭据保护 PowerShell 检查 PASS；5 个脚本 `bash -n` PASS；`git diff --check` PASS。日志保存在 `logs/backup-verifier-tests-current-2026-10-30.log`、`logs/backup-gzip-failure-current-2026-10-30.log`、`logs/backup-credential-guard-current-2026-10-30.log`。此次只测了备份脚本相关项，不把此前全项目测试结果冒充为本轮重跑。
- 本地 Server/Collector JAR SHA256 重算并匹配本地 Manifest；内嵌元数据为 `ca61e5c029f8b86d4f26865a1f8329e7f1305623` / `main` / `dirty=false` / `1.0.0`。它们是本地历史构建产物，不能代表线上部署。

### 生产与线上检查边界

- 用户表示目前无法核对 SSH ED25519 指纹 `SHA256:niEFrPybZrWYn+fCBpwASJFL7M/hwXpqSGalEpWHRew`。没有发起 SSH、生产数据库读取、备份、部署或生产写入；线上 JAR/SHA、服务状态和数据库备份继续 UNKNOWN / NOT RUN。
- 最近一次公开 HTTP 检查仍是 13:12 的 31 个 GET：8 个 PC SPA 路由 HTTP 200，12 个资讯分类过滤均业务成功但总数为 0；没有调用文章详情接口。公开可见内容数仍按前述 13:12 快照记录，未在本补记中再次读取或更新。
- 本地脚本测试不解除生产备份、隔离恢复演练、生产数据审批、管理员审核、图片来源许可、视频播放验证或小程序真机验收等阻塞项。

本补记记录的是本地代码和证据状态，不表示服务器运行版本已确认或内容已写入线上。开发分支提交和推送结果以本补记之后的 Git 核验为准；`main` 未更新。

## 13:31 代码审查修正补记

- 独立只读审查指出：损坏 gzip 在关键表扫描的 `gunzip | awk` 管道返回非零时，会受 `set -e` 影响而直接终止，跳过最终结果摘要。已确认这是实际行为，并为有效归档截去 gzip trailer 的情况增加回归断言。
- TDD 红灯阶段回归失败，输出显示扫描阶段没有 FAIL 行且没有最终 `验证结果` 汇总；修复后验证器将无法完整读取 SQL 计为 FAIL，仍执行最后一项并输出 `2 PASS / 2 FAIL`，回归断言通过。完整验证器样本现在为 3/3 PASS。
- 修复后重新运行：验证器回归 3/3 PASS；gzip 故障注入 PASS；凭据保护检查 PASS；5 个脚本 Bash 语法检查 PASS；`git diff --check` PASS。最新日志 `logs/backup-verifier-tests-current-2026-10-30.log` 已覆盖本次 3 个样本。
- 本段代码和报告尚待提交与推送；上次确认的开发分支和 `main` SHA 仍见 13:28 补记。推送只针对 `origin/codex/phase16-content-quality`，不会改动 `main`。生产连接、备份、数据写入和服务替换仍均未执行。

## 13:35 开发分支推送回执

- 代码与本地验收记录已提交：`b5964b07ce545bbd82611ad357c221eb22953bb3`（`fix(phase16.3): harden backup verification and snapshots`）。
- 使用普通快进推送至 `origin/codex/phase16-content-quality`，远端核对 SHA 与该提交一致。推送前远端开发分支是 `f1a871f368c9a598792a7f897c5f76a3aec0ebe7`；`origin/main` 推送前后均为 `2c75979a8d93729293f20e856ffb4a8c97cf8d62`。没有 force push、合并或修改 `main`。
- 推送后本地 HEAD 与 `origin/codex/phase16-content-quality` 一致，工作区干净。此回执记录本次代码提交的推送；本报告的后续文档修订仍需单独提交推送。

## 13:44 当前推送代码全量本地验收与线上只读复验

### Git 与本地测试

- 当前 HEAD 与 `origin/codex/phase16-content-quality` 均为 `00bcad61c97c724d18f145adc7bdea0520ae40bf`；`origin/main` 仍为 `2c75979a8d93729293f20e856ffb4a8c97cf8d62`。工作区干净。
- Server：208/208，0 failures，0 errors；`logs/server-tests-full-current-2026-10-30.log`。
- Collector：174/174，0 failures，0 errors；`logs/collector-tests-full-current-2026-10-30.log`。
- Web、Admin：Vite 生产构建均 PASS；`logs/web-build-full-current-2026-10-30.log`、`logs/admin-build-full-current-2026-10-30.log`。
- Admin 图片测试 2/2；MiniApp Node 测试 3/3；本地内容 staging importer 11/11；Manifest 测试 PASS（用 PowerShell 7 运行）。
- MiniApp 静态检查：JS 54、JSON 50（全部可解析）、app.json 声明页面 46、缺少 JS/JSON/WXML 文件 0。开发者工具/模拟器/真机仍未运行。
- 备份脚本：有效大备份、缺必需表、损坏 gzip 三个验证器样本通过；gzip 失败关闭回归、凭据保护检查、5 个脚本 Bash 语法检查均 PASS。
- 当前本地 Server/Collector JAR SHA256 与 Manifest 一致，分别为 `854d3690e3b71db58a15950b57104b4a232d0ab688b02398132b2fd59762acbd` 和 `670133f2310e722613f9e673521a300e0f727ea49544f297cd4882e45e293a23`。内嵌 SHA 是历史本地构建 `ca61e5c029f8b86d4f26865a1f8329e7f1305623`，不代表线上部署。

### 线上公开 GET 快照

- 13:43 对 `https://jqr.21zuo.com` 共执行 29 个只读 GET：8 个 PC 路由、9 个公开 API、12 个资讯分类筛选。全部 HTTP 200，API 业务成功；日志 `logs/public-http-current-2026-10-30.log`。未请求资讯详情，也没有任何写请求。
- 当前读取到资讯 37、分类 12（计数全部 0）；产品 56（封面 0）；品牌 47（12 个 Logo 字段非空，但官方性/可访问性未核实）；视频 0、教程 0、社区帖子 0、圈子 9。12 个分类筛选均返回总数 0。
- 这是公开路由和列表 API 的响应证据，不证明服务器进程、数据库、对象存储或实际部署 SHA。SSH 指纹仍无法核验，未登录服务器，生产备份、数据变更、服务替换均未执行；生产插入/更新/删除均为 0。

### 状态

- **Code Ready：** YES（当前推送分支 Server/Collector/Web/Admin/MiniApp 与相关脚本验收如上）。
- **Deployment Verified：** NO（服务器运行 SHA、JAR SHA、systemd 均 UNKNOWN）。
- **Runtime Ready：** PARTIAL（当前公开路由/API 有响应；实际主机服务身份和依赖状态未核实）。
- **Content Ready：** NO（分类 0、产品封面 0、视频/教程/社区帖子 0）。
- **Production Ready：** NO（主机身份、生产备份和内容发布验收仍未完成）。

## 14:02 用户确认后的状态

- 用户目前仍无法从可信宝塔/云控制台核对 ED25519 指纹 `SHA256:niEFrPybZrWYn+fCBpwASJFL7M/hwXpqSGalEpWHRew`。本次没有尝试 SSH，也没有读取生产数据库、创建生产备份或部署。
- 用户同意视频播放按已提出的方案设计：媒体直链走原生播放器；普通平台链接在 PC 打开官方页面，小程序复制官方链接并提示去原平台播放。播放器代码尚未修改，待视频播放设计文档经用户审阅后再进入实现。
- 线上公开数据数字仍以 13:43 的 GET 快照为准；本次没有再次请求线上接口。生产插入、更新、删除仍为 0。

## 14:41 视频播放兜底实施与续验

### 代码与版本

- 用户审阅通过 `docs/superpowers/specs/2026-10-30-video-playback-fallback-design.md` 后，完成 PC 视频详情、PC 机器人视频弹窗及小程序视频详情的直链/平台链接分流。PC 平台链接使用带 `noopener noreferrer` 的用户点击新标签；小程序仅在点击后复制官方链接；直链播放失败时分别提供链接兜底。
- 小程序 URL 分类器原先依赖全局 `URL`。独立审查指出目标逻辑层没有运行时证据；新增无 `URL` 全局的回归用例，先观察到 `none` 与预期 `media` 不符，再改为本地保守校验 HTTP(S)、主机、端口和媒体路径。该修复已包含在 `1ca161a4a27f4aff235569d94d0659fba58b619c`。
- `codex/phase16-content-quality` 本地 HEAD 与远端开发分支已核对为 `1ca161a4a27f4aff235569d94d0659fba58b619c`。从推送前远端 `9269db53b9c5ba6a411dd19963e9b0d07f57d8c5` 普通快进推送成功，未 force push；`origin/main` 推送前后均为 `2c75979a8d93729293f20e856ffb4a8c97cf8d62`。
- Mall 工作区只更新机器人之家视频 URL helper 和 `source-manifest.json` 的 `video-playback-fallback` overlay；未暂存或提交该工作区里的其他改动。Manifest 的基础来源仍是 `4e9f14efb25fc3dbc03f5d049261260d09bd913c`，overlay 来源提交是 `1ca161a4a27f4aff235569d94d0659fba58b619c`。JS/WXML/helper 的源与目标 SHA256 一致；WXSS 目标保留 Mall 的 `@import "../../app.wxss"`，因此只记录其各自哈希，不声称逐字节相同。

### 本轮本地验收

| 范围 | 结果 | 证据 |
|---|---|---|
| MiniApp Node 测试 | 11/11 PASS，包含无 `URL` 全局回归用例 | `logs/miniapp-video-tests-2026-10-30.log` |
| MiniApp JS 语法与 WXML 绑定扫描 | PASS | 上述测试日志及 `node --check` |
| PC 视频测试 | 6/6 PASS | `logs/web-video-tests-2026-10-30.log` |
| Web 生产构建 | PASS | `logs/web-video-build-2026-10-30.log` |
| Admin 图片测试 | 2/2 PASS | `logs/admin-image-video-2026-10-30.log` |
| Admin 生产构建 | PASS | `logs/admin-video-build-2026-10-30.log` |
| Mall 集成测试 | 8/8 PASS，包含 overlay 源/目标哈希核对 | `logs/mall-video-integration-2026-10-30.log` |

- 本轮 Server 208/208 与 Collector 174/174 没有重跑：本次只改 MiniApp URL helper、测试和计划文档，没有后端或采集器文件变化。最近一次全套结果仍是前述 13:44 日志记录；不把它们算作本轮新测。
- 小程序开发者工具、微信模拟器和真机编译/播放测试未运行。Node 模拟仅证明分类器不依赖全局 `URL`，不能代替微信运行时验收。

### 线上边界与最终状态

- 本轮没有 SSH 连接、生产数据库/Redis/MinIO 访问、线上 HTTP 复验、JAR 替换或发布。主机指纹尚未由可信控制台核验；服务器运行 SHA、JAR SHA 和 systemd 状态仍为 `UNKNOWN`。
- 线上内容继续以 13:43 的只读快照为准：资讯 37、已分类资讯 0、产品 56/带封面 0、品牌 47、视频 0、教程 0、社区帖子 0、圈子 9。本轮生产插入、更新和删除均为 0；内容仍未补齐。
- **Code Ready：YES（本次视频兜底实现及本地 Web/Admin/Mall 验收通过；小程序微信工具编译尚未验收）。**
- **Deployment Verified：NO**（本轮未部署，服务器运行版本仍 `UNKNOWN`）。
- **Runtime Ready：PARTIAL**（PC 构建和 Mall 静态集成通过；小程序真实运行环境未测，生产服务状态未核验）。
- **Content Ready：NO**（资讯分类、产品封面、视频、教程和社区内容仍待处理/审核）。
- **Production Ready：NO**。

剩余阻塞仍包括可信 SSH 主机指纹核验、小程序开发者工具/真机验收、生产备份与隔离恢复、逐条内容审核及显式生产写入授权。旧的 13:17/13:43 数据快照没有被本轮结果替代。

## 14:44 报告推送回执

- 视频代码提交 `1ca161a4a27f4aff235569d94d0659fba58b619c` 已先推送。随后阶段报告提交 `25fa1f50e4b88c66758dacdb2ee88d61fcbe4074` 和计划收尾提交 `aeb750ceac6272c89376b1812260c1154cefbb4d` 也都通过普通快进推送到 `origin/codex/phase16-content-quality`。
- 收尾报告提交前最后一次核验显示本地与远端开发分支均为 `aeb750ceac6272c89376b1812260c1154cefbb4d`，工作区干净；`origin/main` 仍为 `2c75979a8d93729293f20e856ffb4a8c97cf8d62`。本条回执通过后续普通提交和推送追加；未改写历史。
