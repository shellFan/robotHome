# Phase16.3 生产部署核验

状态：**BLOCKED，未登录、未部署。**

连接目标为 `49.235.70.249`。用户暂时无法从可信控制台核验 SSH ED25519 主机指纹 `SHA256:niEFrPybZrWYn+fCBpwASJFL7M/hwXpqSGalEpWHRew`，因此没有发起 SSH 连接。

| 证据项 | 状态 |
|---|---|
| Server/Collector 运行 Git SHA | UNKNOWN |
| Server/Collector JAR SHA256 | UNKNOWN |
| 部署时间 | UNKNOWN |
| systemd 与进程健康 | UNKNOWN |
| MySQL、Redis、MinIO 健康 | UNKNOWN |
| 本轮服务替换或重启 | 未执行 |
| 生产数据库写入/备份 | 未执行 |

本地候选包已在代码提交 ca61e5c029f8b86d4f26865a1f8329e7f1305623、branch main、git.dirty=false 下构建。Artifact Version 1.0.0。Manifest docs/phase16.3/deployment-manifest.local.json 标记 environment=local-verification-only、deployTime=NOT_DEPLOYED。Server JAR SHA256 854d3690e3b71db58a15950b57104b4a232d0ab688b02398132b2fd59762acbd；Collector JAR SHA256 670133f2310e722613f9e673521a300e0f727ea49544f297cd4882e45e293a23。生产 JAR/运行版本继续标记 UNKNOWN。

## 公开 HTTPS 只读检查（2026-10-30）

当前 GET 检查日志：`docs/phase16.3/logs/public-http-phase163-current.log`、`public-data-phase163-current.log`、`category-filter-phase163-current.log`。

- PC SPA 路由 `/`、`/robots`、`/brands`、`/articles`、`/videos`、`/tutorials`、`/community`、`/search`：均 HTTP 200。
- 资讯/分类/机器人/品牌/视频/教程/社区帖子/圈子/搜索公开 API：HTTP 200，业务 success=true。
- 当前数据：资讯 37、分类 12（计数均为 0）、机器人 56（公开 API 可确认封面 0）、品牌 47（官方 Logo 核验数 0）、视频 0、教程 0、帖子 0、圈子 9。
- 12 个 `categoryId` 筛选逐一返回 HTTP 200、业务成功、总数 0。
- 未登录 Admin API 的业务响应为 401；没有尝试登录。

HTTP 200 只证明公开路由能响应，不证明页面交互、主机服务、数据库或对象存储健康。未执行服务更新、前端上传、生产写入或基础设施变更。解除阻塞需要可信控制台核验指纹，然后只读采集运行证据；服务替换仍需具体发布授权。

本地 package 与完整测试日志：docs/phase16.3/logs/server-package-2026-10-30.log、collector-package-2026-10-30.log。合并后只读 HTTP 检查仍见报告所列公开路由，但未进行浏览器视觉、Admin 登录或小程序真机验收。

## 2026-10-30 13:28 本地构建产物复核

重新计算本地 target JAR 的 SHA256，Server 为 `854d3690e3b71db58a15950b57104b4a232d0ab688b02398132b2fd59762acbd`，Collector 为 `670133f2310e722613f9e673521a300e0f727ea49544f297cd4882e45e293a23`，与 `deployment-manifest.local.json` 相符。两个 JAR 的 `git.properties` 均记载 SHA `ca61e5c029f8b86d4f26865a1f8329e7f1305623`、branch `main`、`git.dirty=false`；版本均为 `1.0.0`。这是本地历史构建产物证据，不能据此推断服务器运行版本，也不是当前开发分支的新部署。服务器 SHA、JAR SHA、systemd 和依赖服务状态继续为 UNKNOWN；本轮没有 SSH 连接或服务替换。

GitHub 远端只读核验显示开发分支为 `f1a871f368c9a598792a7f897c5f76a3aec0ebe7`，`main` 为 `2c75979a8d93729293f20e856ffb4a8c97cf8d62`。这些是本地复核时的远端值；本地备份脚本修正和报告更新当时尚未提交。

## 2026-10-30 13:43 公开 HTTP 只读复验

当前分支 `00bcad61c97c724d18f145adc7bdea0520ae40bf` 上对 `https://jqr.21zuo.com` 执行 29 个 GET：8 个 PC SPA 路由、9 个公开列表/分类/搜索 API、12 个资讯分类筛选。全部 HTTP 200；公开 API 业务 code 均为 200；搜索热词接口返回成功。日志为 `docs/phase16.3/logs/public-http-current-2026-10-30.log`。

本次读到资讯 37、分类 12（分类计数均 0）；产品 56、非空封面 0；品牌 47，其中 12 个 Logo 字段非空但本次没有验证官方性/可访问性；视频 0、教程 0、社区帖子 0、圈子 9。12/12 分类筛选业务成功但总数均为 0。没有请求资讯详情接口（避免可能增加浏览计数），没有 POST/PUT/DELETE、Admin 登录、生产写入或服务操作。

本地 Server/Collector JAR SHA256 复算仍分别为 `854d3690e3b71db58a15950b57104b4a232d0ab688b02398132b2fd59762acbd` 和 `670133f2310e722613f9e673521a300e0f727ea49544f297cd4882e45e293a23`，与本地 Manifest 相同。内嵌版本元数据仍是本地历史构建的 `ca61e5c029f8b86d4f26865a1f8329e7f1305623` / `main` / `git.dirty=false` / `1.0.0`；生产运行版本依然 UNKNOWN。
