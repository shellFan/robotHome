# Phase14 Night 2 — 进度追踪

**分支**: phase14-commercialization-and-platform-operations
**更新时间**: 2026-10-17

## Stage完成状态

| Stage | 任务 | 状态 |
|-------|------|------|
| 0 | 真实代码审计 → NIGHT2_AUDIT.md | ✅ 完成 |
| 1 | 支付与资金安全专项加固 | ✅ 完成（审计确认合规+2H修复） |
| 2 | 企业商业化工作台 | ✅ 完成（商业概览端点） |
| 3 | 广告投放与统计闭环 | ✅ 完成（限额检查+jumpUrl校验） |
| 4 | 商机推荐与配额 | ✅ 审计确认合规（Phase13已实现） |
| 7 | 通知与商业事件 | ✅ 完成（3类事件接入） |
| 8 | MySQL 5.6数据库验收 | ✅ SQL注释对齐 |
| 12 | 全量回归 | ✅ Server 102/102 PASS |

## Stage 1: 支付与资金安全（审计+修复）

### 审计确认合规项
- ✅ 生产Mock禁用: profile=prod强制isAvailable()=false
- ✅ 支付回调三重防护: 验签→金额防篡改→幂等
- ✅ 订单双唯一索引: uk_order_no + uk_order_idem
- ✅ 价格服务端计算: 从版本取price_cents
- ✅ 退款状态机CAS: APPLYING→SUCCESS/FAIL
- ✅ 退款后撤销权益+降级FREE

### 修复项
- **H-2**: 权益过期链条断裂修复
  - initEntitlements增加expireTime参数（与订阅end_time一致）
  - updateEntitlementExpiry改为实际更新DB（原只打日志）
  - 4处调用点全部传入正确到期时间

## Stage 2: 企业商业化工作台
- EnterpriseWorkspaceController新增 `GET /api/workspace/commercial-overview`
  - 订阅状态（套餐名/状态/到期时间/自动续费）
  - 权益使用概要（entCode/quotaTotal/quotaUsed/status）
  - 订单统计

## Stage 3: 广告投放与统计闭环
- **H-1**: jumpUrl危险协议校验（仅允许http/https，防XSS）
- **M-2**: getSlotAds投放限额检查（totalLimit/dailyLimit超限不展示）

## Stage 7: 通知与商业事件
- 支付成功 → 通知下单用户（eventKey=PAYMENT_SUCCESS:orderNo）
- 退款确认 → 通知下单用户（eventKey=REFUND_CONFIRMED:refundNo）
- 订阅即将到期 → 通知企业OWNER/ADMIN（eventKey=SUBSCRIPTION_EXPIRING:subId）

## Stage 8: SQL修正
- advertising_campaign状态注释: DRAFT/PENDING_REVIEW/APPROVED/REJECTED/ONLINE/OFFLINE
- commercial_refund状态注释+默认值: APPLYING/SUCCESS/FAIL
- commercial_payment状态注释+默认值: INIT/SUCCESS/FAIL

## 全量回归
- Server: **102/102 PASS**（0失败0错误0跳过）
  - Phase14IntegrationTest: 9/9
  - Phase13IntegrationTest: 9/9
  - EnterprisePermissionLifecycleTest: 11/11
  - RobotHomeIntegrationTest: 32/32
  - SchemaContractTest: 4/4
  - SeoRealHttpVerificationTest: 8/8
  - 其他: 29/29