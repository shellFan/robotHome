# Phase14 Night 2 — 代码审计报告

**审计基线**: commit 49ff868 (phase14-commercialization-and-platform-operations)
**审计时间**: 2026-10-17
**审计范围**: commercial包(9entity/9mapper/6service+impl/5controller/2payment/1scheduler) + advertising包 + procurement商机推荐 + SQL schema + 前端契约

## 审计结论

| 级别 | 数量 | 状态 |
|------|------|------|
| CRITICAL | 0 | - |
| HIGH | 2 | 待修复 |
| MEDIUM | 4 | 待修复 |
| LOW | 3 | 待修复 |

## 详细发现

### HIGH-1: jumpUrl危险协议未校验
- **文件**: AdvertisingServiceImpl.createCreative (L248-266)
- **问题**: 接受任意jumpUrl，未校验javascript:/data:/vbscript:等危险协议
- **风险**: XSS攻击向量，违反Night 2安全红线"广告跳转禁危险协议"
- **修复**: 白名单校验，仅允许http://和https://开头

### HIGH-2: 权益过期链条断裂
- **文件**: EntitlementServiceImpl.initEntitlements + CommercialSubscriptionServiceImpl.updateEntitlementExpiry
- **问题1**: initEntitlements创建权益时未设置expireTime字段
- **问题2**: updateEntitlementExpiry方法只打debug日志，未实际更新expire_time
- **风险**: 订阅过期后权益不会自动失效（expireEntitlements的lt(expireTime,now)对NULL不生效），用户可无限使用已过期权益
- **修复**: initEntitlements增加expireTime参数；activateFromOrder/ensureFreeSubscription传入订阅到期时间；updateEntitlementExpiry实际更新DB

### MEDIUM-1: 商业事件未接入通知系统
- **问题**: 支付成功/订阅到期/退款处理等事件仅写commercial_audit_log，用户无站内通知
- **修复**: 关键商业事件调用NotificationService发送用户通知

### MEDIUM-2: getSlotAds不检查投放限额
- **问题**: dailyLimit/totalLimit/budgetCents字段存在但展示时不检查，超限计划仍展示
- **修复**: getSlotAds查询时排除已达限额的计划

### MEDIUM-3: 企业工作台无商业概览
- **问题**: Stage 2要求企业工作台展示商业概览（订阅状态/权益使用/订单统计）
- **修复**: EnterpriseWorkspaceController新增商业概览端点

### MEDIUM-4: MiniApp无商业化联动
- **问题**: Stage 6要求MiniApp展示企业订阅状态
- **修复**: MiniApp企业页展示当前套餐+权益使用概要

### LOW-1: SQL状态注释与代码不一致
- advertising_campaign注释: DRAFT/SUBMITTED/UNDER_REVIEW/APPROVED/REJECTED/ACTIVE/PAUSED/EXPIRED
- 代码实际: DRAFT/PENDING_REVIEW/APPROVED/REJECTED/ONLINE/OFFLINE
- commercial_refund注释: PENDING/CONFIRMED/FAILED → 代码: APPLYING/SUCCESS/FAIL
- commercial_payment注释: PENDING/SUCCESS/FAILED/CLOSED → 代码: INIT/SUCCESS/FAIL

### LOW-2: payment实体状态INIT vs SQL默认PENDING
- 代码显式设置INIT，功能正常，但SQL默认值应改为INIT保持一致

### LOW-3: ORDER_CLOSE_TIMEOUT_MINUTES常量未使用
- CommercialOrderServiceImpl定义了常量但closeExpiredOrders由Scheduler传30

## 合规项确认（PASS）

1. ✅ 生产Mock禁用: MockSandboxPaymentProvider.isAvailable() profile=prod强制false
2. ✅ 支付回调三重防护: 验签→金额防篡改→幂等
3. ✅ 订单双唯一索引幂等: uk_order_no + uk_order_idem
4. ✅ 价格服务端计算: 从版本取price_cents，不接受客户端金额
5. ✅ 退款状态机CAS: APPLYING→SUCCESS/FAIL，成功后撤销权益+降级FREE
6. ✅ 权益配额CAS扣减: WHERE quota_used < quota_total
7. ✅ 广告审核状态机CAS: 每次变更WHERE status=EXPECTED
8. ✅ 广告事件去重: uk_event_dedup (campaignId:eventType:visitorId:date)
9. ✅ 商机推荐配额集成: LEAD_VIEW/LEAD_RESPONSE CAS扣减+竞争失败释放
10. ✅ SQL MySQL 5.6兼容: 无窗口函数/CTE/JSON列，utf8mb4_general_ci
11. ✅ SQL幂等保护: CREATE TABLE IF NOT EXISTS + INSERT WHERE NOT EXISTS
12. ✅ 企业隔离: 订单/投放计划/商机推荐均有companyId校验
13. ✅ 审计日志: 全链路commercial_audit_log记录
14. ✅ 定时任务: 订单超时关闭/订阅到期处理/月度配额重置