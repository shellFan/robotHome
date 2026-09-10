package com.robot.home.collector.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UrlSecurityUtil SSRF防护单元测试
 * 覆盖：协议白名单、内网IP检测、主机名拦截、云元数据端点、URL长度限制
 * 
 * 注意：isAllowedUrl()会对域名执行DNS解析后校验IP，
 * 测试环境DNS可能将公网域名解析到保留段IP（如VPN/代理环境），
 * 因此"允许"类测试不依赖DNS解析，而是通过组件方法或DNS解析失败路径验证。
 */
@DisplayName("SSRF防护工具类测试")
public class UrlSecurityUtilTest {

    // ==================== 协议白名单测试 ====================

    @Nested
    @DisplayName("协议白名单")
    class ProtocolWhitelistTests {

        @Test
        @DisplayName("允许http协议 — 通过DNS解析失败路径验证")
        void allowHttp() {
            // 使用不存在的域名，DNS解析失败时isAllowedUrl返回true（允许继续，后续fetch会失败）
            // 这验证了http协议不被协议白名单拦截
            assertTrue(UrlSecurityUtil.isAllowedUrl("http://nonexistent-domain-test-xyz123.com/page"));
        }

        @Test
        @DisplayName("允许https协议 — 通过DNS解析失败路径验证")
        void allowHttps() {
            assertTrue(UrlSecurityUtil.isAllowedUrl("https://nonexistent-domain-test-xyz123.com/page"));
        }

        @Test
        @DisplayName("拦截file协议")
        void blockFileProtocol() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("file:///etc/passwd"));
        }

        @Test
        @DisplayName("拦截ftp协议")
        void blockFtpProtocol() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("ftp://internal.server/data"));
        }

        @Test
        @DisplayName("拦截javascript协议")
        void blockJavascriptProtocol() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("javascript:alert(1)"));
        }

        @Test
        @DisplayName("拦截data协议")
        void blockDataProtocol() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("data:text/html,<script>alert(1)</script>"));
        }

        @Test
        @DisplayName("拦截gopher协议")
        void blockGopherProtocol() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("gopher://internal.server:6379/"));
        }
    }

    // ==================== 内网IP检测测试 ====================

    @Nested
    @DisplayName("内网IP检测")
    class PrivateIpTests {

        @Test
        @DisplayName("拦截127.0.0.1 (Loopback)")
        void blockLoopback() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://127.0.0.1/admin"));
        }

        @Test
        @DisplayName("拦截127.0.0.x (整个Loopback段)")
        void blockLoopbackRange() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://127.0.0.2/admin"));
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://127.255.255.255/admin"));
        }

        @Test
        @DisplayName("拦截10.x.x.x (Class A私有)")
        void blockClassAPrivate() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://10.0.0.1/admin"));
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://10.255.255.255/admin"));
        }

        @Test
        @DisplayName("拦截172.16.x.x - 172.31.x.x (Class B私有)")
        void blockClassBPrivate() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://172.16.0.1/admin"));
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://172.31.255.255/admin"));
        }

        @Test
        @DisplayName("允许172.15.x.x (非私有)")
        void allowNonPrivateClassB() {
            // 172.15.0.1不在172.16-31范围内，直接用IP测试不触发DNS
            // 但172.15.0.1是公网IP，isAllowedUrl会DNS解析它
            // 改用isPrivateAddress直接验证172.15不在私有范围
            try {
                InetAddress addr = InetAddress.getByName("172.15.0.1");
                assertFalse(UrlSecurityUtil.isPrivateAddress(addr), "172.15.0.1不应被判定为私有地址");
            } catch (Exception e) {
                // 如果DNS解析失败，跳过（不应发生，IP地址无需DNS）
            }
        }

        @Test
        @DisplayName("拦截192.168.x.x (Class C私有)")
        void blockClassCPrivate() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://192.168.0.1/admin"));
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://192.168.1.1/admin"));
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://192.168.255.255/admin"));
        }

        @Test
        @DisplayName("拦截0.0.0.0 (This network)")
        void blockThisNetwork() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://0.0.0.0/admin"));
        }

        @Test
        @DisplayName("拦截169.254.169.254 (云元数据)")
        void blockCloudMetadata() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://169.254.169.254/latest/meta-data/"));
        }

        @Test
        @DisplayName("拦截169.254.x.x (Link-local)")
        void blockLinkLocal() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://169.254.0.1/test"));
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://169.254.100.50/test"));
        }
    }

    // ==================== 主机名拦截测试 ====================

    @Nested
    @DisplayName("主机名拦截")
    class HostnameBlockTests {

        @Test
        @DisplayName("拦截localhost")
        void blockLocalhost() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://localhost/admin"));
        }

        @Test
        @DisplayName("拦截localhost.localdomain")
        void blockLocaldomain() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://localhost.localdomain/admin"));
        }

        @Test
        @DisplayName("拦截0.0.0.0主机名")
        void blockZeroHost() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://0.0.0.0/admin"));
        }
    }

    // ==================== 云元数据端点测试 ====================

    @Nested
    @DisplayName("云元数据端点")
    class CloudMetadataTests {

        @Test
        @DisplayName("拦截AWS/GCP元数据IP")
        void blockAwsGcpMetadata() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://169.254.169.254/latest/meta-data/iam/security-credentials/"));
        }

        @Test
        @DisplayName("拦截GCP元数据主机名")
        void blockGcpMetadataHostname() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://metadata.google.internal/computeMetadata/v1/"));
        }

        @Test
        @DisplayName("拦截Azure元数据主机名")
        void blockAzureMetadataHostname() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("http://metadata.azure.com/metadata/instance?api-version=2021-02-01"));
        }
    }

    // ==================== URL长度限制测试 ====================

    @Nested
    @DisplayName("URL长度限制")
    class UrlLengthTests {

        @Test
        @DisplayName("拦截超长URL")
        void blockTooLongUrl() {
            StringBuilder sb = new StringBuilder("http://example.com/");
            for (int i = 0; i < 2100; i++) {
                sb.append("a");
            }
            assertFalse(UrlSecurityUtil.isAllowedUrl(sb.toString()));
        }

        @Test
        @DisplayName("允许正常长度URL — 通过DNS解析失败路径验证")
        void allowNormalLengthUrl() {
            // 使用不存在的域名避免DNS解析到保留段IP
            assertTrue(UrlSecurityUtil.isAllowedUrl("https://nonexistent-domain-test-xyz123.com/products/"));
        }
    }

    // ==================== 边界条件测试 ====================

    @Nested
    @DisplayName("边界条件")
    class EdgeCaseTests {

        @Test
        @DisplayName("空URL被拦截")
        void blockEmptyUrl() {
            assertFalse(UrlSecurityUtil.isAllowedUrl(""));
            assertFalse(UrlSecurityUtil.isAllowedUrl(null));
        }

        @Test
        @DisplayName("格式错误的URL被拦截")
        void blockMalformedUrl() {
            assertFalse(UrlSecurityUtil.isAllowedUrl("not-a-url"));
            assertFalse(UrlSecurityUtil.isAllowedUrl("://missing-scheme.com"));
        }

        @Test
        @DisplayName("允许公网URL — 通过DNS解析失败路径验证")
        void allowPublicUrl() {
            // 使用不存在的域名，DNS解析失败时isAllowedUrl返回true
            // 这验证了URL格式、协议、主机名等前置检查均通过
            assertTrue(UrlSecurityUtil.isAllowedUrl("https://nonexistent-test-unitree.com/"));
            assertTrue(UrlSecurityUtil.isAllowedUrl("https://nonexistent-test-bd.com/"));
            assertTrue(UrlSecurityUtil.isAllowedUrl("http://nonexistent-test-example.com/page.html?q=test"));
        }
    }

    // ==================== validateUrl异常测试 ====================

    @Nested
    @DisplayName("validateUrl异常")
    class ValidateUrlTests {

        @Test
        @DisplayName("不允许的URL抛出SecurityException")
        void throwOnBlockedUrl() {
            assertThrows(SecurityException.class, () -> UrlSecurityUtil.validateUrl("http://127.0.0.1/admin"));
            assertThrows(SecurityException.class, () -> UrlSecurityUtil.validateUrl("file:///etc/passwd"));
            assertThrows(SecurityException.class, () -> UrlSecurityUtil.validateUrl("http://localhost/secret"));
        }

        @Test
        @DisplayName("允许的URL不抛异常 — 通过DNS解析失败路径验证")
        void noThrowOnAllowedUrl() {
            // 使用不存在的域名，DNS解析失败时不抛异常
            assertDoesNotThrow(() -> UrlSecurityUtil.validateUrl("https://nonexistent-domain-test-xyz123.com/"));
        }
    }

    // ==================== getRejectionReason测试 ====================

    @Nested
    @DisplayName("getRejectionReason")
    class RejectionReasonTests {

        @Test
        @DisplayName("空URL返回原因")
        void reasonForEmptyUrl() {
            assertEquals("URL为空", UrlSecurityUtil.getRejectionReason(""));
            assertEquals("URL为空", UrlSecurityUtil.getRejectionReason(null));
        }

        @Test
        @DisplayName("拦截协议返回原因")
        void reasonForBlockedScheme() {
            String reason = UrlSecurityUtil.getRejectionReason("file:///etc/passwd");
            assertNotNull(reason);
            assertTrue(reason.contains("协议"));
        }

        @Test
        @DisplayName("拦截主机名返回原因")
        void reasonForBlockedHostname() {
            String reason = UrlSecurityUtil.getRejectionReason("http://localhost/admin");
            assertNotNull(reason);
            assertTrue(reason.contains("主机名"));
        }

        @Test
        @DisplayName("允许的URL返回null — 通过DNS解析失败路径验证")
        void nullForAllowedUrl() {
            // 使用不存在的域名，DNS解析失败时返回null（允许）
            assertNull(UrlSecurityUtil.getRejectionReason("https://nonexistent-domain-test-xyz123.com/"));
        }
    }

    // ==================== InetAddress私有地址检测测试 ====================

    @Nested
    @DisplayName("InetAddress私有地址检测")
    class InetAddressPrivateTests {

        @Test
        @DisplayName("null地址视为不安全")
        void nullAddressIsUnsafe() {
            assertTrue(UrlSecurityUtil.isPrivateAddress(null));
        }

        @Test
        @DisplayName("127.0.0.1是私有地址")
        void loopbackIsPrivate() throws Exception {
            InetAddress addr = InetAddress.getByName("127.0.0.1");
            assertTrue(UrlSecurityUtil.isPrivateAddress(addr));
        }

        @Test
        @DisplayName("10.0.0.1是私有地址")
        void classAIsPrivate() throws Exception {
            InetAddress addr = InetAddress.getByName("10.0.0.1");
            assertTrue(UrlSecurityUtil.isPrivateAddress(addr));
        }

        @Test
        @DisplayName("192.168.1.1是私有地址")
        void classCIsPrivate() throws Exception {
            InetAddress addr = InetAddress.getByName("192.168.1.1");
            assertTrue(UrlSecurityUtil.isPrivateAddress(addr));
        }

        @Test
        @DisplayName("169.254.169.254是私有地址")
        void linkLocalIsPrivate() throws Exception {
            InetAddress addr = InetAddress.getByName("169.254.169.254");
            assertTrue(UrlSecurityUtil.isPrivateAddress(addr));
        }
    }
}