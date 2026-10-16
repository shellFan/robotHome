package com.robot.home.commercial.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.commercial.entity.CommercialAuditLog;
import com.robot.home.commercial.entity.CommercialOrder;
import com.robot.home.commercial.entity.CommercialPayment;
import com.robot.home.commercial.mapper.CommercialAuditLogMapper;
import com.robot.home.commercial.mapper.CommercialPaymentMapper;
import com.robot.home.commercial.payment.MockSandboxPaymentProvider;
import com.robot.home.commercial.payment.PaymentProvider;
import com.robot.home.commercial.service.CommercialOrderService;
import com.robot.home.commercial.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付服务实现
 * <p>
 * 核心安全设计：
 * 1. 生产环境Mock支付强制拒绝（MockSandboxPaymentProvider.isAvailable()）
 * 2. 真实支付渠道未配置→标记REAL_PAYMENT_NOT_CONFIGURED
 * 3. 回调验签必须通过才确认订单
 * 4. 回调幂等：已SUCCESS的支付记录不重复处理
 * 5. 金额校验：回调金额必须与订单应付金额一致
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Resource
    private CommercialPaymentMapper paymentMapper;
    @Resource
    private CommercialOrderService orderService;
    @Resource
    private CommercialAuditLogMapper auditLogMapper;
    @Resource
    private MockSandboxPaymentProvider mockSandboxProvider;

    /** 渠道路由表 */
    private final Map<String, PaymentProvider> providerMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 注册Mock沙箱渠道
        registerProvider(mockSandboxProvider);
        // 真实支付渠道（微信/支付宝）未配置
        // 新增真实渠道时在此注册对应Provider实现
        log.info("支付渠道注册完成: channels={}, realPaymentConfigured={}",
                providerMap.keySet(), isRealPaymentConfigured());
    }

    private void registerProvider(PaymentProvider provider) {
        providerMap.put(provider.getChannel(), provider);
    }

    private boolean isRealPaymentConfigured() {
        // 检查是否有非Mock渠道注册
        return providerMap.keySet().stream().anyMatch(c -> !MockSandboxPaymentProvider.CHANNEL.equals(c));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createPayment(Long orderId, Long companyId, String channel) {
        // 校验订单
        CommercialOrder order = orderService.getOrder(orderId, companyId);
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BusinessException("订单状态不支持支付");
        }
        // 渠道路由
        PaymentProvider provider = providerMap.get(channel);
        if (provider == null || !provider.isAvailable()) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "PAYMENT_CHANNEL_UNAVAILABLE");
            err.put("message", "支付渠道不可用或未配置");
            err.put("realPaymentConfigured", isRealPaymentConfigured());
            if (!isRealPaymentConfigured()) {
                err.put("notice", "REAL_PAYMENT_NOT_CONFIGURED: 真实支付渠道尚未配置，请联系平台运营");
            }
            return err;
        }
        // 创建支付记录
        CommercialPayment payment = new CommercialPayment();
        payment.setOrderId(orderId);
        payment.setOrderNo(order.getOrderNo());
        payment.setPaymentChannel(channel);
        payment.setAmountCents(order.getPayableAmountCents());
        payment.setStatus("INIT");
        paymentMapper.insert(payment);

        // 调用渠道创建支付
        String subject = "机器人之家-" + order.getOrderNo();
        Map<String, Object> channelResult = provider.createPayment(
                order.getOrderNo(), order.getPayableAmountCents(), subject);
        // 保存渠道支付单号
        String channelPaymentNo = (String) channelResult.get("channelPaymentNo");
        if (StrUtil.isNotBlank(channelPaymentNo)) {
            payment.setChannelPaymentNo(channelPaymentNo);
            paymentMapper.updateById(payment);
        }
        auditLog("USER", null, companyId, "PAYMENT_CREATE", "PAYMENT", payment.getId(),
                "channel=" + channel + ",orderNo=" + order.getOrderNo());
        // 合并返回
        Map<String, Object> result = new HashMap<>(channelResult);
        result.put("paymentId", payment.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("amountCents", order.getPayableAmountCents());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> handleCallback(String channel, Map<String, String> params, String rawBody) {
        Map<String, Object> result = new HashMap<>();
        // 渠道路由
        PaymentProvider provider = providerMap.get(channel);
        if (provider == null) {
            result.put("success", false);
            result.put("message", "未知支付渠道: " + channel);
            return result;
        }
        // 验签
        PaymentProvider.CallbackVerifyResult verifyResult = provider.verifyCallback(params, rawBody);
        String orderNo = params.get("orderNo");
        // 记录回调
        CommercialPayment payment = paymentMapper.selectOne(
                Wrappers.<CommercialPayment>lambdaQuery()
                        .eq(CommercialPayment::getOrderNo, orderNo)
                        .eq(CommercialPayment::getPaymentChannel, channel)
                        .orderByDesc(CommercialPayment::getId)
                        .last("LIMIT 1"));
        if (payment != null) {
            payment.setCallbackRaw(rawBody);
            payment.setVerifyResult(verifyResult.getMessage());
            payment.setCallbackTime(LocalDateTime.now());
            if (!verifyResult.isSuccess()) {
                payment.setStatus("FAIL");
                paymentMapper.updateById(payment);
            }
        }
        if (!verifyResult.isSuccess()) {
            result.put("success", false);
            result.put("message", "验签失败: " + verifyResult.getMessage());
            auditLog("SYSTEM", null, null, "PAYMENT_CALLBACK_FAIL", "PAYMENT",
                    payment != null ? payment.getId() : null,
                    "channel=" + channel + ",reason=" + verifyResult.getMessage());
            return result;
        }
        // 幂等：检查支付记录是否已SUCCESS
        if (payment != null && "SUCCESS".equals(payment.getStatus())) {
            result.put("success", true);
            result.put("message", "已处理，幂等返回");
            result.put("idempotent", true);
            return result;
        }
        // 查订单
        CommercialOrder order = orderService.getByOrderNo(orderNo);
        if (order == null) {
            result.put("success", false);
            result.put("message", "订单不存在: " + orderNo);
            return result;
        }
        // 金额校验（防篡改）
        String amountStr = params.get("amountCents");
        if (StrUtil.isNotBlank(amountStr)) {
            try {
                long callbackAmount = Long.parseLong(amountStr);
                if (callbackAmount != order.getPayableAmountCents()) {
                    result.put("success", false);
                    result.put("message", "回调金额与订单不一致");
                    auditLog("SYSTEM", null, order.getCompanyId(), "PAYMENT_AMOUNT_MISMATCH", "PAYMENT",
                            payment != null ? payment.getId() : null,
                            "expected=" + order.getPayableAmountCents() + ",got=" + callbackAmount);
                    return result;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        // 更新支付记录SUCCESS
        if (payment != null) {
            payment.setStatus("SUCCESS");
            payment.setChannelPaymentNo(verifyResult.getChannelPaymentNo());
            paymentMapper.updateById(payment);
        }
        // 确认订单（幂等）
        orderService.confirmPayment(order.getId(), channel, verifyResult.getChannelPaymentNo());
        result.put("success", true);
        result.put("message", "支付成功");
        auditLog("SYSTEM", null, order.getCompanyId(), "PAYMENT_CALLBACK_SUCCESS", "PAYMENT",
                payment != null ? payment.getId() : null, "orderNo=" + orderNo);
        return result;
    }

    @Override
    public List<CommercialPayment> getPaymentsByOrderNo(String orderNo) {
        return paymentMapper.selectList(
                Wrappers.<CommercialPayment>lambdaQuery()
                        .eq(CommercialPayment::getOrderNo, orderNo)
                        .orderByDesc(CommercialPayment::getId));
    }

    @Override
    public List<Map<String, Object>> listAvailableChannels() {
        List<Map<String, Object>> channels = new ArrayList<>();
        for (Map.Entry<String, PaymentProvider> entry : providerMap.entrySet()) {
            Map<String, Object> ch = new HashMap<>();
            ch.put("channel", entry.getKey());
            ch.put("available", entry.getValue().isAvailable());
            if (MockSandboxPaymentProvider.CHANNEL.equals(entry.getKey())) {
                ch.put("sandbox", true);
                ch.put("notice", "MOCK_SANDBOX: 仅沙箱测试");
            }
            channels.add(ch);
        }
        if (!isRealPaymentConfigured()) {
            Map<String, Object> notice = new HashMap<>();
            notice.put("channel", "REAL_PAYMENT");
            notice.put("available", false);
            notice.put("notice", "REAL_PAYMENT_NOT_CONFIGURED");
            channels.add(notice);
        }
        return channels;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sandboxConfirmPayment(Long orderId, Long companyId) {
        // 仅沙箱环境可用
        if (!mockSandboxProvider.isAvailable()) {
            throw new BusinessException("沙箱支付未启用，无法模拟确认（生产环境禁止）");
        }
        CommercialOrder order = orderService.getOrder(orderId, companyId);
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BusinessException("订单状态不支持支付确认");
        }
        // 模拟回调参数
        String mockNo = "MOCK_" + System.currentTimeMillis() + "_" + order.getOrderNo();
        Map<String, String> params = new HashMap<>();
        params.put("orderNo", order.getOrderNo());
        params.put("mockNo", mockNo);
        params.put("status", "SUCCESS");
        params.put("amountCents", String.valueOf(order.getPayableAmountCents()));
        // 计算Mock签名
        String sign = cn.hutool.crypto.digest.DigestUtil.md5Hex(
                order.getOrderNo() + mockNo + "MOCK_SANDBOX_SECRET");
        params.put("sign", sign);
        // 走标准回调流程
        return handleCallback(MockSandboxPaymentProvider.CHANNEL, params,
                "sandbox-confirm:" + order.getOrderNo());
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