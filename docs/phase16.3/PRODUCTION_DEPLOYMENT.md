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

本地 Maven 测试资源记录 branch `codex/phase16-content-quality`、SHA `9d60e1f18687146c1a5b8270ca5c2a17192a2148`、dirty=true、Artifact Version 1.0.0。它们是本地测试构建元数据，不是生产 JAR 或部署证明。线上运行版本继续标记 UNKNOWN。

## 公开 HTTPS 只读检查（2026-10-30）

当前 GET 检查日志：`docs/phase16.3/logs/public-http-phase163-current.log`、`public-data-phase163-current.log`、`category-filter-phase163-current.log`。

- PC SPA 路由 `/`、`/robots`、`/brands`、`/articles`、`/videos`、`/tutorials`、`/community`、`/search`：均 HTTP 200。
- 资讯/分类/机器人/品牌/视频/教程/社区帖子/圈子/搜索公开 API：HTTP 200，业务 success=true。
- 当前数据：资讯 37、分类 12（计数均为 0）、机器人 56（公开 API 可确认封面 0）、品牌 47（官方 Logo 核验数 0）、视频 0、教程 0、帖子 0、圈子 9。
- 12 个 `categoryId` 筛选逐一返回 HTTP 200、业务成功、总数 0。
- 未登录 Admin API 的业务响应为 401；没有尝试登录。

HTTP 200 只证明公开路由能响应，不证明页面交互、主机服务、数据库或对象存储健康。未执行服务更新、前端上传、生产写入或基础设施变更。解除阻塞需要可信控制台核验指纹，然后只读采集运行证据；服务替换仍需具体发布授权。
