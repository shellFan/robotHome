# Phase14 Night 3 进度报告

**分支**: `phase14-commercialization-and-platform-operations`
**日期**: 2026-10-18
**状态**: Night 3 全部可执行 Stage 完成

---

## 一、提交记录

| Commit | 说明 |
|--------|------|
| `9617752` | feat(commercial): Web商业概览接入+企业广告管理+MiniApp商业化全链路 (22文件, 1279插入) |
| `65a7f8b` | feat(commercial): Admin商业统计看板+安全回归测试+E2E补齐+权益过期链条修复 (7文件, 731插入) |

两个 commit 均已推送至 `origin/phase14-commercialization-and-platform-operations`。

---

## 二、Stage 完成状态

### ✅ 已完成

| Stage | 内容 | 验证 |
|-------|------|------|
| Stage 1 | Web 商业概览接入（订阅状态/广告位/套餐展示） | 编译PASS |
| Stage 2 | 企业广告管理 + MiniApp 商业化全链路 | 提交9617752 |
| Stage 3 | 广告闭环后端验证（限额展示过滤+事件去重） | 代码审计确认已完整 |
| Stage 4 | 商机配额 CRM 联动验证（LEAD_VIEW/LEAD_RESPONSE消耗+CAS竞争） | 代码审计确认已完整 |
| Stage 5 | Admin 商业统计端点 + 前端看板 | Server编译PASS + Admin构建PASS(42.41s) |
| Stage 6 | Phase14SecurityRegressionTest（5个安全红线测试） | 5/5 PASS |
| Stage 7 | Phase14IntegrationTest 新增3个E2E | 12/12 PASS |
| Stage 10 | Server 全量回归 | **110/110 PASS (2:53min)** |
| Stage 11 | 本进度文档 + 最终提交推送 | commit 65a7f8b |

### ⛔ 环境限制（预期阻塞）

| Stage | 限制码 | 原因 |
|-------|--------|------|
| Stage 8 | `MYSQL56_RUNTIME_BLOCKED` | 本地无 MySQL 5.6 实例，Fresh/Upgrade验证依赖H2兼容测试已覆盖 |
| Stage 9 | `NGINX_RUNTIME_BLOCKED` | 本地无 Nginx 环境，反向代理配置已通过docker-compose.prod.yml定义 |
| Stage 9 | `MINIAPP_DEVICE_BLOCKED` | 无微信开发者工具/真机环境，MiniApp代码已通过构建验证 |

---

## 三、关键安全修复

### 权益过期链条完整性修复（EntitlementServiceImpl）

**缺陷**：原 `expireEntitlements` 含 `.lt(expireTime, now)` 条件——订阅到期时，若权益的 `expireTime` 基于订阅时长计算仍在未来，权益不会被撤销，导致订阅失效后权益仍可用。

**修复**：移除 `expireTime` 条件，订阅到期/退款时**无条件撤销该企业所有 ACTIVE 权益**。语义：订阅失效 = 权益失效，无论 expireTime。

```java
// 修复后
entitlementMapper.update(null,
        Wrappers.<EnterpriseEntitlement>lambdaUpdate()
                .set(EnterpriseEntitlement::getStatus, "EXPIRED")
                .eq(EnterpriseEntitlement::getCompanyId, companyId)
                .eq(EnterpriseEntitlement::getStatus, "ACTIVE"));
```

**测试覆盖**：`subscriptionExpirationExpiresEntitlements` E2E 完整验证支付→到期→权益撤销→配额阻断全链条。

---

## 四、Admin 商业统计设计

### 收入 REAL/SANDBOX 严格分离

```java
SANDBOX_CHANNELS = [MOCK_SANDBOX, MOCK, SANDBOX, DEMO]
```

- 沙箱渠道金额计入 `sandboxRevenueCents`，**绝不冒充** `realRevenueCents`
- 前端看板：REAL 收入绿色卡片 / SANDBOX 收入黄色卡片 / 合计卡片
- 渠道收入明细表格：沙箱渠道显示 `沙箱` 标签

### 统计结构

- `orderStats`: total / pendingPayment / paid / closed / refunded
- `revenueStats`: realRevenueCents / sandboxRevenueCents / totalRevenueCents / channelRevenue
- `subscriptionStats`: total / active / suspended / expired
- `refundStats`: total / applying / success / failed

---

## 五、测试清单

### Phase14SecurityRegressionTest（5/5 PASS）

1. `jumpUrlDangerousProtocolsAreRejected` — javascript:/data:/vbscript:/大小写绕过→拦截；http/https→通过
2. `expiredEntitlementIsNotUsable` — expireTime过去→requireEntitlement抛"过期"→consumeQuota失败
3. `advertisingTotalLimitIsEnforced` — totalLimit=1→1次IMPRESSION→不再展示
4. `advertisingDailyLimitIsEnforced` — dailyLimit=1→1次IMPRESSION→当日不再展示
5. `notificationEventKeyIsIdempotent` — 同eventKey→仅1条消息

### Phase14IntegrationTest 新增 E2E（3/3 PASS）

1. `subscriptionExpirationExpiresEntitlements` — 完整支付→ACTIVE→模拟到期→processExpirations→EXPIRED+权益撤销+配额阻断
2. `duplicateCallbackIsIdempotent` — 重复回调→订单保持PAID+仅1条SUCCESS支付记录
3. `adminCommercialStatsEndpointReturnsCorrectStructure` — generateAdminToken+MockMvc→4个统计结构+REAL/SANDBOX分离

### 全量回归

- **Server: 110/110 PASS** (含 Phase14 全部 + 历史全部)
- Admin 构建 PASS (42.41s)
- Web 构建 PASS

---

## 六、环境限制说明

1. **Collector 测试**：Collector 为 Java Maven 项目（含 PublishIntegrationTest 等），本 Night 未执行 `mvn test`，Phase12/13 已验证通过（129 tests），本次无 Collector 代码变更。
2. **MySQL 5.6**：本地仅 H2 兼容模式，Fresh/Upgrade SQL 验证由 H2 + SchemaInitializer 覆盖。
3. **Nginx/MiniApp 真机**：docker-compose.prod.yml 配置已就绪，真机验证待部署环境。

---

## 七、下一步

- Night 3 交付完成，等待用户验收
- 合并 main 需用户确认后执行（当前禁止自主 merge）