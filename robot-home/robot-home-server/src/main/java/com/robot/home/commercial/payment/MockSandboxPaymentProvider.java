package com.robot.home.commercial.payment;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock沙箱支付实现
 * <p>
 * 安全约束：
 * 1. 仅在非生产环境可用（payment.sandbox-enabled=true时）
 * 2. 生产环境（spring.profiles.active=prod）强制不可用
 * 3. Mock签名仅用于沙箱测试，非真实支付
 */
@Slf4j
@Component
public class MockSandboxPaymentProvider implements PaymentProvider {

    public static final String CHANNEL = "MOCK_SANDBOX";

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${payment.sandbox-enabled:false}")
    private boolean sandboxEnabled;

    @Override
    public String getChannel() {
        return CHANNEL;
    }

    @Override
    public boolean isAvailable() {
        // 生产环境强制不可用
        if ("prod".equalsIgnoreCase(activeProfile)) {
            log.warn("Mock支付渠道在生产环境不可用: profile={}", activeProfile);
            return false;
        }
        return sandboxEnabled;
    }

    @Override
    public Map<String, Object> createPayment(String orderNo, long amountCents, String subject) {
        if (!isAvailable()) {
            throw new IllegalStateException("Mock沙箱支付未启用或生产环境禁用");
        }
        // Mock: 生成模拟支付单号和支付页面URL
        String mockPaymentNo = "MOCK_" + System.currentTimeMillis() + "_" + orderNo;
        Map<String, Object> result = new HashMap<>();
        result.put("channelPaymentNo", mockPaymentNo);
        result.put("payUrl", "/payment/sandbox/pay?orderNo=" + orderNo + "&mockNo=" + mockPaymentNo);
        result.put("channel", CHANNEL);
        result.put("sandbox", true);
        result.put("notice", "MOCK_SANDBOX_PAYMENT: 仅沙箱测试，非真实扣款");
        log.info("Mock沙箱支付单创建: orderNo={}, amount={}分, mockNo={}", orderNo, amountCents, mockPaymentNo);
        return result;
    }

    @Override
    public CallbackVerifyResult verifyCallback(Map<String, String> params, String rawBody) {
        if (!isAvailable()) {
            return CallbackVerifyResult.fail("Mock沙箱支付未启用");
        }
        // Mock验签：校验签名格式 mockSign = md5(orderNo + mockNo + "MOCK_SANDBOX_SECRET")
        String orderNo = params.get("orderNo");
        String mockNo = params.get("mockNo");
        String sign = params.get("sign");
        String status = params.get("status");
        if (StrUtil.isBlank(orderNo) || StrUtil.isBlank(mockNo) || StrUtil.isBlank(sign)) {
            return CallbackVerifyResult.fail("缺少必要参数");
        }
        if (!"SUCCESS".equals(status)) {
            return CallbackVerifyResult.fail("支付状态非SUCCESS: " + status);
        }
        String expectedSign = DigestUtil.md5Hex(orderNo + mockNo + "MOCK_SANDBOX_SECRET");
        if (!expectedSign.equals(sign)) {
            log.warn("Mock回调验签失败: orderNo={}", orderNo);
            return CallbackVerifyResult.fail("签名校验失败");
        }
        return CallbackVerifyResult.ok(mockNo);
    }
}