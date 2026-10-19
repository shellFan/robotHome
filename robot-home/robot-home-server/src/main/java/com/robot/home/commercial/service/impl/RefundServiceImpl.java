package com.robot.home.commercial.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.commercial.entity.CommercialAuditLog;
import com.robot.home.commercial.entity.CommercialOrder;
import com.robot.home.commercial.entity.CommercialRefund;
import com.robot.home.commercial.mapper.CommercialAuditLogMapper;
import com.robot.home.commercial.mapper.CommercialOrderMapper;
import com.robot.home.commercial.mapper.CommercialRefundMapper;
import com.robot.home.commercial.service.RefundService;
import com.robot.home.commercial.service.CommercialSubscriptionService;
import com.robot.home.message.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 退款服务实现
 * <p>
 * 核心安全设计：
 * 1. 仅PAID订单可退款
 * 2. 退款确认幂等：已SUCCESS的退款不重复处理
 * 3. 退款成功后CAS更新订单状态为REFUNDED
 * 4. 撤销权益通过SubscriptionService.handleRefund
 */
@Slf4j
@Service
public class RefundServiceImpl implements RefundService {

    private static final DateTimeFormatter REFUND_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private CommercialRefundMapper refundMapper;
    @Resource
    private CommercialOrderMapper orderMapper;
    @Resource
    private CommercialAuditLogMapper auditLogMapper;
    @Resource
    private CommercialSubscriptionService subscriptionService;
    @Resource
    private NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommercialRefund applyRefund(Long orderId, Long operatorId, String reason) {
        CommercialOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new BusinessException("仅已支付订单可退款，当前状态: " + order.getStatus());
        }
        // 幂等：检查是否已有进行中的退款
        CommercialRefund existing = refundMapper.selectOne(
                Wrappers.<CommercialRefund>lambdaQuery()
                        .eq(CommercialRefund::getOrderId, orderId)
                        .in(CommercialRefund::getStatus, "APPLYING", "SUCCESS"));
        if (existing != null) {
            throw new BusinessException("订单已有进行中或已完成的退款: " + existing.getRefundNo());
        }
        CommercialRefund refund = new CommercialRefund();
        refund.setOrderId(orderId);
        refund.setRefundNo(generateRefundNo());
        refund.setAmountCents(order.getPayableAmountCents());
        refund.setStatus("APPLYING");
        refund.setReason(reason);
        refund.setOperatorId(operatorId);
        refundMapper.insert(refund);
        auditLog("ADMIN", operatorId, order.getCompanyId(), "REFUND_APPLY", "REFUND", refund.getId(),
                "orderId=" + orderId + ",amount=" + order.getPayableAmountCents() + ",reason=" + reason);
        log.info("退款申请: refundNo={}, orderId={}, amount={}", refund.getRefundNo(), orderId, refund.getAmountCents());
        return refund;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmRefund(Long refundId, Long operatorId) {
        CommercialRefund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException("退款记录不存在");
        }
        // 幂等：已SUCCESS直接返回
        if ("SUCCESS".equals(refund.getStatus())) {
            log.info("退款已确认，幂等返回: refundId={}", refundId);
            return;
        }
        if (!"APPLYING".equals(refund.getStatus())) {
            throw new BusinessException("退款状态不支持确认: " + refund.getStatus());
        }
        // CAS更新退款状态
        int updated = refundMapper.update(null,
                Wrappers.<CommercialRefund>lambdaUpdate()
                        .set(CommercialRefund::getStatus, "SUCCESS")
                        .set(CommercialRefund::getConfirmTime, LocalDateTime.now())
                        .eq(CommercialRefund::getId, refundId)
                        .eq(CommercialRefund::getStatus, "APPLYING"));
        if (updated == 0) {
            throw new BusinessException("退款确认竞争失败，请重试");
        }
        // CAS更新订单状态为REFUNDED
        CommercialOrder order = orderMapper.selectById(refund.getOrderId());
        if (order != null) {
            int orderUpdated = orderMapper.update(null,
                    Wrappers.<CommercialOrder>lambdaUpdate()
                            .set(CommercialOrder::getStatus, "REFUNDED")
                            .eq(CommercialOrder::getId, order.getId())
                            .eq(CommercialOrder::getStatus, "PAID"));
            if (orderUpdated > 0) {
                // 撤销权益+降级订阅（仅当订单CAS成功，防止重复退款处理）
                subscriptionService.handleRefund(order.getCompanyId(), order.getId());
            } else {
                log.warn("订单状态CAS失败(可能已退款)，跳过权益撤销: orderId={}", order.getId());
            }
        }
        auditLog("ADMIN", operatorId, order != null ? order.getCompanyId() : null,
                "REFUND_CONFIRM", "REFUND", refundId, "refundNo=" + refund.getRefundNo());
        log.info("退款确认成功: refundId={}, orderId={}", refundId, refund.getOrderId());
        // 发送用户通知（退款已处理）
        if (order != null) {
            try {
                notificationService.sendNotification(order.getUserId(), "SYSTEM",
                        "退款已处理", "订单" + order.getOrderNo() + "退款已确认，订阅已降级为免费版",
                        "company", order.getCompanyId(),
                        "REFUND_CONFIRMED:" + refund.getRefundNo());
            } catch (Exception e) {
                log.warn("退款确认通知发送失败: refundNo={}", refund.getRefundNo(), e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void failRefund(Long refundId, String reason) {
        CommercialRefund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException("退款记录不存在");
        }
        if (!"APPLYING".equals(refund.getStatus())) {
            throw new BusinessException("仅申请中退款可标记失败");
        }
        refundMapper.update(null,
                Wrappers.<CommercialRefund>lambdaUpdate()
                        .set(CommercialRefund::getStatus, "FAIL")
                        .set(CommercialRefund::getReason, refund.getReason() + " | 失败原因: " + reason)
                        .eq(CommercialRefund::getId, refundId)
                        .eq(CommercialRefund::getStatus, "APPLYING"));
        CommercialOrder order = orderMapper.selectById(refund.getOrderId());
        auditLog("ADMIN", null, order != null ? order.getCompanyId() : null,
                "REFUND_FAIL", "REFUND", refundId, reason);
        log.info("退款标记失败: refundId={}, reason={}", refundId, reason);
    }

    @Override
    public List<CommercialRefund> getRefundsByOrderId(Long orderId) {
        return refundMapper.selectList(
                Wrappers.<CommercialRefund>lambdaQuery()
                        .eq(CommercialRefund::getOrderId, orderId)
                        .orderByDesc(CommercialRefund::getId));
    }

    @Override
    public PageResult<CommercialRefund> adminListRefunds(String status, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<CommercialRefund> page = new Page<>(pn, ps);
        Page<CommercialRefund> result = refundMapper.selectPage(page,
                Wrappers.<CommercialRefund>lambdaQuery()
                        .eq(status != null && !status.isEmpty(), CommercialRefund::getStatus, status)
                        .orderByDesc(CommercialRefund::getId));
        return PageResult.of(pn, ps, result.getTotal(), result.getRecords());
    }

    private String generateRefundNo() {
        return "RF" + LocalDateTime.now().format(REFUND_NO_FMT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
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