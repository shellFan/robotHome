package com.robot.home.commercial.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.commercial.entity.CommercialAuditLog;
import com.robot.home.commercial.entity.CommercialOrder;
import com.robot.home.commercial.entity.EnterprisePlan;
import com.robot.home.commercial.entity.EnterprisePlanVersion;
import com.robot.home.commercial.mapper.CommercialAuditLogMapper;
import com.robot.home.commercial.mapper.CommercialOrderMapper;
import com.robot.home.commercial.mapper.EnterprisePlanMapper;
import com.robot.home.commercial.mapper.EnterprisePlanVersionMapper;
import com.robot.home.commercial.service.CommercialOrderService;
import com.robot.home.commercial.service.CommercialSubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 商业订单服务实现
 * <p>
 * 核心安全设计：
 * 1. 价格服务端计算：从套餐活跃版本取price_cents，不接受客户端金额
 * 2. 幂等：uk_order_no + uk_order_idem双唯一索引
 * 3. 状态机CAS：WHERE status='EXPECTED'防止非法跳转
 * 4. 支付确认幂等：已PAID直接返回，不重复开通权益
 */
@Slf4j
@Service
public class CommercialOrderServiceImpl implements CommercialOrderService {

    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int ORDER_CLOSE_TIMEOUT_MINUTES = 30;

    @Resource
    private CommercialOrderMapper orderMapper;
    @Resource
    private EnterprisePlanMapper planMapper;
    @Resource
    private EnterprisePlanVersionMapper planVersionMapper;
    @Resource
    private CommercialAuditLogMapper auditLogMapper;
    @Resource
    private CommercialSubscriptionService subscriptionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommercialOrder createOrder(Long companyId, Long userId, Long planId, String idempotentKey) {
        if (companyId == null || userId == null || planId == null) {
            throw new BusinessException("参数不完整");
        }
        if (StrUtil.isBlank(idempotentKey)) {
            throw new BusinessException("幂等键不能为空");
        }
        // 幂等：同幂等键直接返回已有订单
        CommercialOrder existing = orderMapper.selectOne(
                Wrappers.<CommercialOrder>lambdaQuery()
                        .eq(CommercialOrder::getIdempotentKey, idempotentKey));
        if (existing != null) {
            // 安全校验：幂等键对应的订单必须属于当前企业
            if (!existing.getCompanyId().equals(companyId)) {
                throw new BusinessException("无权操作此订单");
            }
            return existing;
        }
        // 校验套餐可售
        EnterprisePlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("套餐不存在");
        }
        if (!"ON_SHELF".equals(plan.getStatus()) || plan.getSaleable() != 1) {
            throw new BusinessException("套餐当前不可购买");
        }
        // 获取活跃版本
        EnterprisePlanVersion version = planVersionMapper.selectOne(
                Wrappers.<EnterprisePlanVersion>lambdaQuery()
                        .eq(EnterprisePlanVersion::getPlanId, planId)
                        .eq(EnterprisePlanVersion::getVersion, plan.getCurrentVersion())
                        .eq(EnterprisePlanVersion::getStatus, "ACTIVE"));
        if (version == null) {
            throw new BusinessException("套餐没有可购买的版本");
        }
        // 价格由服务端从版本计算
        long priceCents = version.getPriceCents() == null ? 0 : version.getPriceCents();

        CommercialOrder order = new CommercialOrder();
        order.setOrderNo(generateOrderNo());
        order.setCompanyId(companyId);
        order.setUserId(userId);
        order.setPlanId(planId);
        order.setPlanVersionId(version.getId());
        // 套餐快照：名称+版本+价格+权益+有效期
        order.setPlanSnapshot(buildPlanSnapshot(plan, version));
        order.setOriginalPriceCents(priceCents);
        order.setPayableAmountCents(priceCents);
        order.setCurrency("CNY");
        order.setStatus("PENDING_PAYMENT");
        order.setIdempotentKey(idempotentKey);
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // 并发创建：幂等返回已有订单
            CommercialOrder concurrent = orderMapper.selectOne(
                    Wrappers.<CommercialOrder>lambdaQuery()
                            .eq(CommercialOrder::getIdempotentKey, idempotentKey));
            if (concurrent != null) {
                return concurrent;
            }
            throw new BusinessException("订单创建失败，请重试");
        }
        auditLog("USER", userId, companyId, "ORDER_CREATE", "ORDER", order.getId(),
                "orderNo=" + order.getOrderNo() + ",amount=" + priceCents);
        log.info("创建订单: orderNo={}, companyId={}, planId={}, amount={}",
                order.getOrderNo(), companyId, planId, priceCents);
        return order;
    }

    @Override
    public CommercialOrder getOrder(Long orderId, Long companyId) {
        CommercialOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        // 企业隔离：只能查自己的订单
        if (!order.getCompanyId().equals(companyId)) {
            throw new BusinessException("无权查看此订单");
        }
        return order;
    }

    @Override
    public CommercialOrder getByOrderNo(String orderNo) {
        return orderMapper.selectOne(
                Wrappers.<CommercialOrder>lambdaQuery()
                        .eq(CommercialOrder::getOrderNo, orderNo));
    }

    @Override
    public PageResult<CommercialOrder> listCompanyOrders(Long companyId, String status, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<CommercialOrder> page = new Page<>(pn, ps);
        IPage<CommercialOrder> result = orderMapper.selectPage(page,
                Wrappers.<CommercialOrder>lambdaQuery()
                        .eq(CommercialOrder::getCompanyId, companyId)
                        .eq(StrUtil.isNotBlank(status), CommercialOrder::getStatus, status)
                        .orderByDesc(CommercialOrder::getCreateTime));
        return PageResult.of(pn, ps, result.getTotal(), result.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long companyId) {
        CommercialOrder order = getOrder(orderId, companyId);
        // 状态机：仅PENDING_PAYMENT可取消
        int updated = orderMapper.update(null,
                Wrappers.<CommercialOrder>lambdaUpdate()
                        .set(CommercialOrder::getStatus, "CANCELLED")
                        .set(CommercialOrder::getCloseTime, LocalDateTime.now())
                        .eq(CommercialOrder::getId, orderId)
                        .in(CommercialOrder::getStatus, "CREATED", "PENDING_PAYMENT"));
        if (updated == 0) {
            throw new BusinessException("当前订单状态不可取消");
        }
        auditLog("USER", null, companyId, "ORDER_CANCEL", "ORDER", orderId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPayment(Long orderId, String paymentChannel, String paymentNo) {
        CommercialOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        // 幂等：已PAID直接返回（重复回调安全）
        if ("PAID".equals(order.getStatus())) {
            log.info("订单已支付，幂等返回: orderId={}", orderId);
            return;
        }
        // 状态机CAS：PENDING_PAYMENT→PAID
        int updated = orderMapper.update(null,
                Wrappers.<CommercialOrder>lambdaUpdate()
                        .set(CommercialOrder::getStatus, "PAID")
                        .set(CommercialOrder::getPaymentChannel, paymentChannel)
                        .set(CommercialOrder::getPaymentNo, paymentNo)
                        .set(CommercialOrder::getPayTime, LocalDateTime.now())
                        .eq(CommercialOrder::getId, orderId)
                        .eq(CommercialOrder::getStatus, "PENDING_PAYMENT"));
        if (updated == 0) {
            // 竞争：可能已取消/已过期
            CommercialOrder current = orderMapper.selectById(orderId);
            if (current != null && "CANCELLED".equals(current.getStatus())) {
                throw new BusinessException("订单已取消，支付确认失败（超时关闭与支付成功竞争）");
            }
            throw new BusinessException("订单状态不支持支付确认");
        }
        auditLog("SYSTEM", null, order.getCompanyId(), "PAYMENT_CONFIRM", "ORDER", orderId,
                "channel=" + paymentChannel + ",paymentNo=" + paymentNo);
        // 激活订阅+权益（幂等）
        try {
            subscriptionService.activateFromOrder(order);
        } catch (Exception e) {
            log.error("支付确认后激活订阅失败: orderId={}", orderId, e);
            // 不回滚支付状态，由重试任务恢复
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredOrders(int timeoutMinutes) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        // 查询超时未支付订单
        java.util.List<CommercialOrder> expired = orderMapper.selectList(
                Wrappers.<CommercialOrder>lambdaQuery()
                        .in(CommercialOrder::getStatus, "CREATED", "PENDING_PAYMENT")
                        .lt(CommercialOrder::getCreateTime, deadline)
                        .last("LIMIT 100"));
        int closed = 0;
        for (CommercialOrder order : expired) {
            int updated = orderMapper.update(null,
                    Wrappers.<CommercialOrder>lambdaUpdate()
                            .set(CommercialOrder::getStatus, "EXPIRED")
                            .set(CommercialOrder::getCloseTime, LocalDateTime.now())
                            .eq(CommercialOrder::getId, order.getId())
                            .in(CommercialOrder::getStatus, "CREATED", "PENDING_PAYMENT"));
            if (updated > 0) {
                closed++;
                auditLog("SYSTEM", null, order.getCompanyId(), "ORDER_EXPIRE", "ORDER", order.getId(), null);
            }
        }
        if (closed > 0) {
            log.info("关闭超时订单: {}条", closed);
        }
        return closed;
    }

    @Override
    public PageResult<CommercialOrder> adminListOrders(String status, Long companyId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<CommercialOrder> page = new Page<>(pn, ps);
        IPage<CommercialOrder> result = orderMapper.selectPage(page,
                Wrappers.<CommercialOrder>lambdaQuery()
                        .eq(StrUtil.isNotBlank(status), CommercialOrder::getStatus, status)
                        .eq(companyId != null, CommercialOrder::getCompanyId, companyId)
                        .orderByDesc(CommercialOrder::getCreateTime));
        return PageResult.of(pn, ps, result.getTotal(), result.getRecords());
    }

    @Override
    public CommercialOrder adminGetOrder(Long orderId) {
        CommercialOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    // ==================== 私有方法 ====================

    private String generateOrderNo() {
        // 时间戳+6位随机数，保证唯一性（配合uk_order_no）
        return "CO" + LocalDateTime.now().format(ORDER_NO_FMT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }

    private String buildPlanSnapshot(EnterprisePlan plan, EnterprisePlanVersion version) {
        return "{\"planCode\":\"" + plan.getCode()
                + "\",\"planName\":\"" + plan.getName()
                + "\",\"version\":" + version.getVersion()
                + ",\"priceCents\":" + version.getPriceCents()
                + ",\"durationDays\":" + version.getDurationDays()
                + ",\"entitlementConfig\":" + (version.getEntitlementConfig() != null ? version.getEntitlementConfig() : "[]")
                + "}";
    }

    private void auditLog(String operatorType, Long operatorId, Long companyId,
                          String action, String entityType, Long entityId, String detail) {
        try {
            CommercialAuditLog audit = new CommercialAuditLog();
            audit.setOperatorType(operatorType);
            audit.setOperatorId(operatorId);
            audit.setCompanyId(companyId);
            audit.setAction(action);
            audit.setEntityType(entityType);
            audit.setEntityId(entityId);
            audit.setDetail(detail);
            auditLogMapper.insert(audit);
        } catch (Exception e) {
            log.warn("审计日志写入失败: {}", e.getMessage());
        }
    }
}