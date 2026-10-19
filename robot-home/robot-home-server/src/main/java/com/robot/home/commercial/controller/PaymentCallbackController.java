package com.robot.home.commercial.controller;

import com.robot.home.common.Result;
import com.robot.home.commercial.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付回调接口（渠道webhook）
 * <p>
 * 安全：
 * 1. 验签必须通过才确认订单（PaymentService.handleCallback）
 * 2. 生产环境Mock渠道回调被拒绝
 * 3. 回调幂等处理
 */
@RestController
@RequestMapping("/api/payment/callback")
public class PaymentCallbackController {

    @Resource
    private PaymentService paymentService;

    /**
     * 通用支付回调（POST form参数）
     */
    @PostMapping("/{channel}")
    public Result<Map<String, Object>> handleCallback(
            @PathVariable String channel,
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {
        String rawBody = readRawBody(request);
        Map<String, Object> result = paymentService.handleCallback(channel, params, rawBody);
        boolean success = Boolean.TRUE.equals(result.get("success"));
        if (success) {
            return Result.success(result);
        }
        return Result.error(String.valueOf(result.get("message")));
    }

    /**
     * Mock沙箱回调（仅沙箱环境，测试用）
     */
    @PostMapping("/mock-sandbox")
    public Result<Map<String, Object>> mockSandboxCallback(
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {
        String rawBody = readRawBody(request);
        Map<String, Object> result = paymentService.handleCallback("MOCK_SANDBOX", params, rawBody);
        boolean success = Boolean.TRUE.equals(result.get("success"));
        if (success) {
            return Result.success(result);
        }
        return Result.error(String.valueOf(result.get("message")));
    }

    private String readRawBody(HttpServletRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}