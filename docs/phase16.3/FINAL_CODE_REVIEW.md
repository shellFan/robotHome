# Phase16.3 最终代码审查

审查时间：2026-10-30（Asia/Shanghai）

## 范围与结果

审查了开发分支相对 `origin/main` 的 12 个提交，以及本地未提交的 Collector、Admin、MiniApp、部署脚本和内容审计材料。对 Collector 的采集发布事务、正文提取、内容类型识别、HTTP 重定向、DNS 校验、浏览器网络出口和资源生命周期做了重点复核。

先前 DNS rebinding 风险来自校验阶段与连接阶段各自解析域名。当前 Collector 将连接解析限制在 `SafeDnsResolver` 校验过的地址；HTTP 重定向逐跳验证；OkHttp 禁用隐式 JVM 系统代理；应用显式代理配置无法保证目标 DNS 固定时 fail closed。Playwright 请求由同一安全 HTTP 传输代取，Service Worker 被禁用，启动时配置 loopback 丢弃代理拦截路由遗漏的出站连接。启动失败会关闭 Playwright 和代理资源。

本轮复现并修复了两个生命周期/出口问题：浏览器启动失败时未关闭已创建的 Playwright 实例；OkHttp 默认代理选择器可能将 DNS 解析交给代理。对应回归测试均先失败再修复。全量 Collector 测试 **174/174 PASS**，包括安全 DNS、系统代理、浏览器路由/出口、发布事务与内容识别测试。

在已审查的 Collector 出站抓取路径中，原 SSRF HIGH 已关闭；这不等于完整渗透测试。Server/Collector 仍有未提交改动，构建元数据记录 `git.dirty=true`。未生成或部署生产 JAR。SQL 资源静态模式扫描未发现已知 MySQL 5.7+ 专属语法，但没有 MySQL 5.6 实例做运行时验证。

## 验收边界

- Server：208/208 PASS。
- Web/Admin：Vite 生产构建 PASS；Admin 图片 fallback 测试 2/2 PASS。
- MiniApp：54 个 JS、50 个 JSON、46 个声明页面的静态检查通过；测试 3/3 PASS。微信开发者工具和真机未验收。
- 公开 HTTPS 页面/API 可返回；浏览器自动化两次超时，未取得视觉或交互验收证据。后台鉴权 API 未登录返回业务 401。
- 主机身份未核验，生产备份、MySQL/Redis/MinIO 健康、生产 JAR、支付沙箱和私有文件权限未检查。

## 发布判定

- 已审查抓取路径已知 BLOCKER：0。
- 已审查抓取路径 HIGH：0；其他代码和运行环境未完成全面安全审计。
- 本地构建/测试：通过。
- 合并 main：不执行，工作区仍有用户未提交改动，尚未完成完整的发布前审查。
- 生产部署：未执行；服务器指纹未核验，且没有服务替换授权。

## Additional local verification — 2026-10-30 12:00 Asia/Shanghai

- Backup pipeline root cause: the backup script captured only PIPESTATUS[0], so a successful mysqldump followed by a failed gzip could be reported as success. A mocked disk/write failure reproduced this before the fix. Both Docker and standalone branches now capture the whole pipeline status array, fail if either command fails, and delete the incomplete artifact.
- Regression evidence: test-backup-gzip-failure.sh failed before the fix and passes after it. The recovery suite reports 27 PASS / 0 FAIL / 0 SKIP. Credential guard and deployment manifest tests pass. These are local tests only; no production backup or restore was executed.
- Admin install-script policy: pnpm 11.25.0 now has explicit allowBuilds: false decisions for es5-ext, esbuild, and vue-demi; no package scripts were broadly approved. The es5-ext hook is an optional notice, esbuild can use its packaged platform binary, and vue-demi ships the Vue 3 entry point. An isolated frozen-lockfile install plus Pinia/Vue and esbuild runtime checks passed; the Admin Vite production build also passed. The first offline-only install attempt could not run because the local pnpm store lacked a package, so the same isolated install was rerun online and passed.