package com.robot.home.commercial.payment;

import java.util.Map;

/**
 * 支付渠道适配接口
 * <p>
 * 新增真实支付渠道时实现此接口（如 WechatPayProvider/AlipayProvider）。
 * 实现类必须注册为Spring Bean，PaymentService按channel自动路由。
 */
public interface PaymentProvider {

    /**
     * 渠道标识（如 MOCK_SANDBOX / WECHAT / ALIPAY）
     */
    String getChannel();

    /**
     * 是否可用（生产环境Mock渠道返回false）
     */
    boolean isAvailable();

    /**
     * 创建支付单（返回渠道侧参数，如支付链接/二维码等）
     *
     * @param orderNo    订单号
     * @param amountCents 金额(分)
     * @param subject    商品描述
     * @return 渠道侧支付参数（如 payUrl, channelPaymentNo等）
     */
    Map<String, Object> createPayment(String orderNo, long amountCents, String subject);

    /**
     * 验证回调签名
     *
     * @param params 回调参数
     * @param rawBody 原始报文
     * @return 验签结果（SUCCESS / FAIL + 失败原因）
     */
    CallbackVerifyResult verifyCallback(Map<String, String> params, String rawBody);

    /**
     * 回调验签结果
     */
    class CallbackVerifyResult {
        private final boolean success;
        private final String message;
        private final String channelPaymentNo;

        public CallbackVerifyResult(boolean success, String message, String channelPaymentNo) {
            this.success = success;
            this.message = message;
            this.channelPaymentNo = channelPaymentNo;
        }

        public static CallbackVerifyResult ok(String channelPaymentNo) {
            return new CallbackVerifyResult(true, "SUCCESS", channelPaymentNo);
        }

        public static CallbackVerifyResult fail(String reason) {
            return new CallbackVerifyResult(false, reason, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getChannelPaymentNo() { return channelPaymentNo; }
    }
}