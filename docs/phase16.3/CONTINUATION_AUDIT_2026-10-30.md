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
