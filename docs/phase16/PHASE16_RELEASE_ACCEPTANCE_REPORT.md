# Phase16 当前发布验收报告

验收日期：2026-10-28（Asia/Shanghai）
代码仓库：`shellFan/robotHome`
开发分支：`codex/phase16-content-quality`

## 五项发布状态

| 状态项 | 结果 | 依据 |
|---|---|---|
| Code Ready | CONDITIONAL | Server、Collector 测试与 Web、Admin 生产构建通过；MiniApp JavaScript/JSON 检查通过。未安装微信开发者工具，无法完成开发者工具编译。 |
| Deployment Verified | NO | 服务器运行的 JAR 与 systemd 服务已核对，构建元数据没有 Git SHA，运行提交只能标记为 `UNKNOWN`。 |
| Runtime Ready | PARTIAL | systemd 服务处于运行状态，PC 首页与代表性只读 API HTTPS 返回 200；内容缺失和图片覆盖率未达发布要求。 |
| Content Ready | NO | 产品图片、资讯分类、视频、教程和社区帖子存在已确认缺口；未写入生产数据库。 |
| Production Ready | NO | 部署代码身份未知，内容验收不通过，生产数据补全尚未执行。 |

## 版本对账

- 对账基准提交：`16110e511e4655c508eb3c57d4ad4fba8aef1520`。
- GitHub `origin/main`：`4e9f14efb25fc3dbc03f5d049261260d09bd913c`。
- 对比：本地基准提交相对 `origin/main` 领先 1 个提交、落后 0 个提交。
- 工作分支：`codex/phase16-content-quality`；工作区未清理，原有改动均予保留。未跟踪的 `robot-home/phase15_diff_stat.txt` 和 `robot-home/phase15_name_status.txt` 未纳入本次提交。
- 本阶段代码与验收报告已提交：`d3c12c0`（`fix(collector): improve content classification and release safety`）。
- 推送状态：**BLOCKED**。HTTPS Git push 需要交互式 GitHub 凭据，但当前终端没有可用的凭据对话；GitHub SSH 也未通过主机密钥验证。提交保留在本地开发分支，未改动远程 `main`。
- 线上 Server 与 Collector 的 systemd ExecStart 均指向 `/www/wwwroot/robot-home/releases/20261023-launch/` 下的 JAR。SHA256 分别为：
  - Server：`ad33f87e7287bca434849dcf031581461f8b8b991c6dd055197428f803d378b9`
  - Collector：`dcacb894e76dc34a83aa2cb20cf8d15d8aba0b98975f98bbf2d2c71b03185aa2`
- JAR 元数据确认 Java 8、Spring Boot 2.7.18、应用版本 1.0.0；未发现 `git.properties` 或 `build-info.properties`。服务器正在运行的 Git SHA：**UNKNOWN**。部署目录、日志及 JAR 校验和只能识别工件，不能证明其来源提交。
- 线上 systemd：`robot-home-server.service`、`robot-home-collector.service` 均为 active。服务器根分区剩余约 158 MB；本阶段没有在服务器构建、发布或改动基础设施。

## 未提交改动与处理

本分支原有改动包含数据库启动安全守卫、Server/Collector Flyway 隔离、Springfox 与生产运行兼容配置、采集器调度显式开关、发布回归测试及 V19 迁移测试。已复核采集单条发布事务仍使用独立 `REQUIRES_NEW`、CAS 防重复和失败重试路径；测试使用本地 H2，不写线上库。

Phase16 收尾新增/修正：

- 分类器识别 DOM 产品页优先于来源配置的 `article` 默认值，减少产品详情混入资讯。
- 采集适配器排除 privacy、cookie、terms、legal、policy 等非编辑内容 URL；手工、RSS、sitemap 种子进入队列前也走同一过滤器。
- Cookie 同意层清理增加正文块保护，避免短页面中因祖先节点包含 Cookie 字样而整段删除正常正文。
- 回归覆盖 Cookie 覆盖层、法律页面排除和 DOM 产品页类型判定。
- 分类映射复核发现：旧线上 37 条资讯的 `category_id` 均为 NULL；12 个公开资讯分类的计数因此全部为 0。已有采集发布器对新发布内容提供默认分类 ID 1，但这不能补齐既有线上行，也无法证明旧数据是由当前采集发布代码写入。本阶段未执行生产回填。

未跟踪文件 `robot-home/phase15_diff_stat.txt`、`robot-home/phase15_name_status.txt` 原样保留且不纳入提交；`docs/superpowers/plans/2026-10-28-phase16-content-quality.md` 与本报告属于 Phase16 文档。

## 自动化验证

| 组件 | 验证 | 结果 |
|---|---|---|
| Collector | Maven 全量测试 | 149 passed，0 failed，0 errors |
| Server | Maven 全量测试 | 207 passed，0 failed，0 errors |
| Web | Vite production build | 1,947 modules transformed，build 成功 |
| Admin | Vite production build | 2,365 modules transformed，build 成功 |
| MiniApp | Node `--check` 与 JSON 配置解析 | 52 个 JS 文件、50 个 JSON 文件通过 |
| MiniApp | 微信开发者工具编译 | BLOCKED：本机未检测到微信开发者工具 CLI/可执行程序 |

Web/Admin 使用桌面随附 Node 与 Vite 直接构建；系统未配置 npm。初始 pnpm 调用生成的临时 pnpm 锁文件已移除，没有纳入提交。

## 线上内容只读验收

生产 MySQL 5.7.44（TCP 3309）统计通过只读事务查询；数据库未执行写入。数据项如下：

| 数据 | 当前数量/状态 | 验收结果 |
|---|---:|---|
| 已发布资讯 | 37 | 37 条 `category_id` 为空；12 个分类 API 计数均为 0。至少 4 条标题为 Cookie/隐私/政策内容，至少 21 条呈产品详情特征；已见正文包含 Cookie 同意层或产品规格页内容。 |
| 机器人产品 | 56 | 39 条状态 1，17 条状态 2；56 条均无封面，`robot_image` 记录为 0。 |
| 品牌 | 47 | 状态 1。 |
| 视频 / 视频分类 | 0 / 0 | 空。 |
| 教程 / 教程分类 | 0 / 0 | 空。 |
| 社区帖子 | 0 | 空；圈子 9 个。没有创建虚构用户、帖子或互动。 |
| crawler_media | 0 | 没有已归档采集媒体。 |
| UBTECH 采集记录 | 40 | 37 条 `PUBLISHED`，3 条 `DUPLICATE`；来源未配置 sitemap、包含/排除规则或页面选择器，基础入口允许全站链接发现。 |

根因证据：分类统计按 `article.category_id` 分组，线上行的分类外键全部为空；公开内容中混入产品页与隐私页，与采集来源未设置 URL 范围和产品 DOM 类型未覆盖来源默认类型相符；产品封面字段和 `robot_image` 表均为空。当前图片 HEAD 检查曾对 UBTECH 站点图片返回 403，仍需以真实 GET/来源策略确认防盗链影响。线上历史数据的原始写入路径尚不能仅凭现存数据证明。

**生产数据修复前后数量：未修改，前后相同。** 生产数据的备份、dry-run、真实来源导入和回填方案尚待用户授权后执行；本阶段没有伪造社区内容或填入占位媒体。

## HTTPS 实测

2026-10-28 对线上域名执行只读 GET：

| 请求 | HTTP | 结果 |
|---|---:|---|
| `https://jqr.21zuo.com/` | 200 | PC 首页 HTML 可访问，title 为“机器人之家 - 机器人产品库 · 品牌库 · 企业库 · 资讯 · 参数对比”。 |
| `/api/articles` | 200 | 返回总数 37，第一页 20 条。 |
| `/api/robots` | 200 | 返回总数 56，第一页 20 条。 |
| `/api/brands` | 200 | 返回总数 47，第一页 20 条。 |
| `/api/articles/categories` | 200 | 12 个分类，全部计数为 0。 |
| `/api/videos`、`/api/tutorials` | 200 | 两者总数均为 0。 |
| `/api/community/posts`、`/api/community/circles` | 200 | 帖子总数 0；圈子 9 个。 |

HTTPS API 与 PC 页面在线可达不代表小程序开发者工具编译通过，也不证明线上工件对应某个 Git 提交。

## BLOCKED 与待处理

1. 微信开发者工具未安装，MiniApp 只能完成 JS/JSON 静态校验。
2. 服务器工件没有 Git 构建元数据，运行 SHA 为 `UNKNOWN`；未重新部署以补充证明。
3. 生产内容数据仍需补充封面及真实可追溯的产品、资讯、视频、教程；视频应使用合法嵌入或官方跳转。
4. 资讯分类回填、坏内容下架/修正和批量采集导入涉及生产数据库写入，须先备份、dry-run，并在用户确认授权后执行。
5. 服务器根分区剩余约 158 MB，不应在该服务器运行构建或部署任务。

## 当前结论

当前仅完成代码与运行环境的审计及代码侧修复；生产数据未修改、服务未替换、基础设施未部署。对外发布仍为 **NOT READY**。
