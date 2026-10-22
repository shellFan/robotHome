# Phase15 Night 5 — Runtime BLOCKED 项汇总报告

> Stage 2/4/5/6/7/9 | 日期: 2026-10-22 | 验证等级: BLOCKED

## 1. BLOCKED 原因

本地开发环境不具备Runtime验证条件：
- Docker不可用（未安装或未启动）
- WSL2不可用
- 无MySQL/Redis/MinIO/Nginx运行实例

**不假PASS原则**：所有需要真实运行时环境的验证项标记为BLOCKED，同时提供可在Docker机器上执行的完整脚本和验证步骤。

## 2. 各Stage状态与执行指南

### Stage 2: MySQL 5.6→5.7 升级排练 — BLOCKED

**目标**：验证Flyway迁移在MySQL 5.7上从零执行+从5.6备份恢复升级

**已有资产**：
- `deploy/phase15/compose.rehearsal.yml` — 双MySQL实例(13306 fresh / 13307 upgrade)
- `sql/` — Flyway迁移脚本
- `scripts/phase15/preflight.sh` — 19项前置检查含SQL链验证

**执行步骤**（Docker机器上）：
```bash
bash scripts/phase15/run-rehearsal.sh          # 启动排练环境
bash scripts/phase15/verify-runtime.sh          # 验证5组件运行时
# Fresh: 验证Flyway从零迁移
docker exec robot-home-rehearsal-mysql mysql -uroot -p robot_home -e "SELECT COUNT(*) FROM flyway_schema_history;"
# Upgrade: 导入5.6备份→验证兼容性
docker exec robot-home-rehearsal-mysql-upgrade mysql -uroot -p robot_home < backup_5.6.sql
bash scripts/phase15/cleanup-rehearsal.sh       # 清理
```

### Stage 4: Nginx配置验证 — BLOCKED

**目标**：验证nginx.conf安全头+限流+私有文件不直接暴露

**已有资产**：
- `deploy/nginx.conf` — HTTP配置（安全头+分级限流+Swagger/Actuator屏蔽）
- `deploy/nginx-https.conf` — HTTPS配置（SSL+HSTS）

**静态验证已通过**（CONFIG_VALIDATED）：
- [x] server_tokens off 隐藏版本号
- [x] X-Content-Type-Options/X-Frame-Options/X-XSS-Protection/Referrer-Policy
- [x] Swagger/Actuator端点返回404
- [x] API分级限流(api_limit 20r/s / auth_limit 5r/s / payment_limit 50r/s)
- [x] /files/ 代理到后端（私有文件授权由PrivateFileController+PrivateFileAccessService执行）
- [x] 无直接静态文件location暴露private目录

**Runtime待验证**：
```bash
# 在排练环境启动后
docker exec robot-home-rehearsal-nginx nginx -t                    # 配置语法
curl -I http://localhost:18080/api/actuator                        # 期望404
curl -I http://localhost:18080/files/private/test.jpg              # 期望401/403
curl -I http://localhost:18080/                                    # 期望安全头存在
```

### Stage 5: 真实采集HTTP探测 — BLOCKED

**目标**：验证CrawlerEngine.startTask()真实HTTP请求执行

**已有资产**：
- `robot-home-collector/` — 采集服务（129+测试PASS）
- Night 5前已完成CanaryRunner真实执行验证（commit a7f435c）

**Runtime执行**：
```bash
# 启动Server+Collector后
curl -X POST http://localhost:8081/api/admin/crawler/start -H "Authorization: Bearer $TOKEN"
# 观察Collector日志中真实HTTP请求和结果落库
```

### Stage 6: 商业化E2E — BLOCKED

**目标**：验证套餐购买→订单→订阅→权益→退款全链路

**已有资产**：
- `Phase14IntegrationTest.java` — 商业化集成测试
- `CommercialSchedulerTest.java` — 商业化调度测试
- `CommercialFailureRecoveryTest.java` — 商业化故障恢复测试
- `ProcurementLifecycleIntegrationTest.java` — 采购生命周期测试

**单元/集成测试已通过**（UNIT_TEST_PASS）：这些测试使用H2内存数据库+Mock支付回调，覆盖业务逻辑但不含真实HTTP/支付通道。

**Runtime待验证**：
```bash
# 启动完整环境后
curl -X POST http://localhost:8081/api/commercial/plan/list        # 套餐列表
curl -X POST http://localhost:8081/api/commercial/order/create     # 创建订单(mock支付)
# 验证权益开通+Web/Admin前端展示
```

### Stage 7: 三端联调 — BLOCKED

**目标**：Web+Admin+MiniApp三端与Server API联调

**已有资产**：
- `robot-home-web/` — PC前端（build PASS）
- `robot-home-admin/` — 管理后台（build PASS）
- `robot-home-miniapp/` — 小程序（46页×4文件 ALL PASS）

**Runtime待验证**：
- Web端登录→浏览→搜索→详情页
- Admin端登录→审核→统计→配置
- MiniApp端（需微信开发者工具）

### Stage 9: 性能故障注入 — BLOCKED

**目标**：验证Redis故障降级+慢查询+并发安全

**已有资产**：
- Phase10/12已完成的并发安全测试和Redis韧性测试
- `NIGHT3_FINAL_RELEASE_GATE.md` 中性能相关检查项

**Runtime待验证**：
```bash
# Redis故障降级
docker stop robot-home-rehearsal-redis
curl http://localhost:8081/api/public/robots     # 应降级到DB查询不报错
docker start robot-home-rehearsal-redis
```

## 3. 排练环境组件清单

| 组件 | 端口 | 用途 |
|------|------|------|
| MySQL Fresh | 13306 | 从零Flyway迁移验证 |
| MySQL Upgrade | 13307 | 5.6备份恢复升级验证 |
| Redis | 16379 | 缓存+会话 |
| MinIO Primary | 19000 | 对象存储 |
| MinIO Secondary | 19001 | MinIO恢复验证 |
| Nginx | 18080 | 反向代理+安全头 |

## 4. 一键排练命令

```bash
# 完整排练流程（在Docker机器上执行）
bash scripts/phase15/preflight.sh               # Step 1: 前置检查
bash scripts/phase15/run-rehearsal.sh            # Step 2: 启动排练环境
bash scripts/phase15/verify-runtime.sh           # Step 3: 运行时验证
bash scripts/phase15/test-backup-recovery.sh     # Step 4: 备份恢复测试
# ... 执行各Stage的Runtime验证 ...
bash scripts/phase15/cleanup-rehearsal.sh        # Step N: 清理
```

## 5. BLOCKED项汇总

| Stage | 名称 | 验证等级 | 阻塞原因 |
|-------|------|----------|----------|
| 2 | MySQL升级排练 | BLOCKED | Docker不可用 |
| 4 | Nginx验证 | CONFIG_VALIDATED | Docker不可用(Runtime部分) |
| 5 | 真实采集 | BLOCKED | 需运行时环境 |
| 6 | 商业化E2E | UNIT_TEST_PASS | 需运行时环境 |
| 7 | 三端联调 | BUILD_PASS | 需运行时环境 |
| 9 | 性能故障 | BLOCKED | 需运行时环境 |