package com.robot.home.collector.util;

import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * URL安全工具类 — SSRF防护
 * 
 * 防护策略：
 * 1. 协议白名单：仅允许 http/https
 * 2. 内网IP检测：禁止访问私有IP段（127.0.0.0/8, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 169.254.0.0/16）
 * 3. 特殊主机名拦截：localhost, 0.0.0.0, [::1]等
 * 4. URL长度限制：最大2048字符
 * 5. DNS Rebinding基础防护：解析域名后二次校验IP
 * 6. 云元数据端点拦截：169.254.169.254 (AWS/GCP/Azure元数据服务)
 */
public class UrlSecurityUtil {

    /** 允许的协议 */
    private static final Set<String> ALLOWED_SCHEMES = new HashSet<>(Arrays.asList("http", "https"));

    /** URL最大长度 */
    private static final int MAX_URL_LENGTH = 2048;

    /** 云元数据端点IP */
    private static final String CLOUD_METADATA_IP = "169.254.169.254";

    /** 
     * 私有IP网段（CIDR表示）
     * 127.0.0.0/8   - Loopback
     * 10.0.0.0/8    - Class A private
     * 172.16.0.0/12 - Class B private (172.16.0.0 - 172.31.255.255)
     * 192.168.0.0/16 - Class C private
     * 169.254.0.0/16 - Link-local (含云元数据)
     * 0.0.0.0/8     - "This network"
     * 100.64.0.0/10 - Carrier-grade NAT (RFC 6598)
     * fc00::/7      - IPv6 unique local
     * fe80::/10     - IPv6 link-local
     * ::1           - IPv6 loopback
     */

    /** 拦截的主机名（不区分大小写） */
    private static final Set<String> BLOCKED_HOSTNAMES = new HashSet<>(Arrays.asList(
            "localhost",
            "localhost.localdomain",
            "0.0.0.0",
            "[::1]",
            "::1",
            "ip6-localhost",
            "ip6-loopback"
    ));

    /** IPv4私有网段范围检测 */
    private static boolean isPrivateIPv4(byte[] addr) {
        if (addr == null || addr.length != 4) return false;

        int first = addr[0] & 0xFF;
        int second = addr[1] & 0xFF;

        // 0.0.0.0/8 — "This network"
        if (first == 0) return true;

        // 127.0.0.0/8 — Loopback
        if (first == 127) return true;

        // 10.0.0.0/8 — Class A private
        if (first == 10) return true;

        // 172.16.0.0/12 — Class B private (172.16.x.x - 172.31.x.x)
        if (first == 172 && second >= 16 && second <= 31) return true;

        // 192.168.0.0/16 — Class C private
        if (first == 192 && second == 168) return true;

        // 169.254.0.0/16 — Link-local (含云元数据 169.254.169.254)
        if (first == 169 && second == 254) return true;

        // 100.64.0.0/10 — Carrier-grade NAT
        if (first == 100 && (second & 0xC0) == 0x40) return true;

        return false;
    }

    /** IPv6私有地址检测 */
    private static boolean isPrivateIPv6(byte[] addr) {
        if (addr == null || addr.length != 16) return false;

        // ::1 — Loopback
        boolean isLoopback = true;
        for (int i = 0; i < 15; i++) {
            if (addr[i] != 0) { isLoopback = false; break; }
        }
        if (isLoopback && addr[15] == 1) return true;

        // fc00::/7 — Unique local (fd00::/8 and fc00::/8)
        if ((addr[0] & 0xFE) == 0xFC) return true;

        // fe80::/10 — Link-local
        if (addr[0] == (byte) 0xFE && (addr[1] & 0xC0) == 0x80) return true;

        return false;
    }

    /**
     * 检测IP地址是否为私有/内网地址
     * @param addr InetAddress对象
     * @return true=内网地址，false=公网地址
     */
    public static boolean isPrivateAddress(InetAddress addr) {
        if (addr == null) return true; // null视为不安全
        byte[] bytes = addr.getAddress();
        if (bytes.length == 4) {
            return isPrivateIPv4(bytes);
        } else if (bytes.length == 16) {
            return isPrivateIPv6(bytes);
        }
        return true; // 未知地址类型视为不安全
    }

    /**
     * 检测主机名是否在拦截名单中
     * @param host 主机名
     * @return true=被拦截
     */
    public static boolean isBlockedHostname(String host) {
        if (StringUtils.isBlank(host)) return true;
        return BLOCKED_HOSTNAMES.contains(host.toLowerCase());
    }

    /**
     * 检测URL是否为云元数据端点
     * @param host 主机名或IP
     * @return true=云元数据端点
     */
    public static boolean isCloudMetadataEndpoint(String host) {
        if (StringUtils.isBlank(host)) return false;
        return CLOUD_METADATA_IP.equals(host) ||
               host.endsWith(".169.254.169.254") ||
               host.equalsIgnoreCase("metadata.google.internal") ||
               host.equalsIgnoreCase("metadata.azure.com");
    }

    /**
     * 校验URL是否允许访问（SSRF防护主入口）
     * 
     * 检查项：
     * 1. URL非空且长度合法
     * 2. 协议白名单（http/https）
     * 3. 主机名拦截（localhost等）
     * 4. 云元数据端点拦截
     * 5. DNS解析后IP校验（防止DNS Rebinding基础攻击）
     * 
     * @param url 待校验的URL
     * @return true=允许访问，false=禁止访问
     */
    public static boolean isAllowedUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }

        // 1. URL长度限制
        if (url.length() > MAX_URL_LENGTH) {
            return false;
        }

        // 2. 解析URL
        URL parsedUrl;
        try {
            parsedUrl = new URL(url);
        } catch (MalformedURLException e) {
            return false;
        }

        // 3. 协议白名单
        String scheme = parsedUrl.getProtocol();
        if (!ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            return false;
        }

        // 4. 获取主机名
        String host = parsedUrl.getHost();
        if (StringUtils.isBlank(host)) {
            return false;
        }

        // 5. 拦截主机名（localhost, 0.0.0.0等）
        if (isBlockedHostname(host)) {
            return false;
        }

        // 6. 云元数据端点拦截
        if (isCloudMetadataEndpoint(host)) {
            return false;
        }

        // 7. DNS解析后IP校验（DNS Rebinding基础防护）
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (isPrivateAddress(addr)) {
                    return false;
                }
            }
        } catch (UnknownHostException e) {
            // DNS解析失败 — 允许继续（后续fetch会失败）
        }

        return true;
    }

    /**
     * 校验URL，不允许则抛出异常
     * @param url 待校验的URL
     * @throws SecurityException URL不允许访问
     */
    public static void validateUrl(String url) {
        if (!isAllowedUrl(url)) {
            throw new SecurityException("SSRF protection: URL not allowed: " + url);
        }
    }

    /**
     * 校验URL，不允许则返回安全错误信息（不抛异常）
     * @param url 待校验的URL
     * @return null表示安全，否则返回错误原因
     */
    public static String getRejectionReason(String url) {
        if (StringUtils.isBlank(url)) {
            return "URL为空";
        }
        if (url.length() > MAX_URL_LENGTH) {
            return "URL超过最大长度" + MAX_URL_LENGTH;
        }

        URL parsedUrl;
        try {
            parsedUrl = new URL(url);
        } catch (MalformedURLException e) {
            return "URL格式错误: " + e.getMessage();
        }

        String scheme = parsedUrl.getProtocol();
        if (!ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            return "不允许的协议: " + scheme;
        }

        String host = parsedUrl.getHost();
        if (StringUtils.isBlank(host)) {
            return "URL缺少主机名";
        }
        if (isBlockedHostname(host)) {
            return "被拦截的主机名: " + host;
        }
        if (isCloudMetadataEndpoint(host)) {
            return "云元数据端点: " + host;
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (isPrivateAddress(addr)) {
                    return "解析到内网IP: " + addr.getHostAddress();
                }
            }
        } catch (UnknownHostException e) {
            return null; // DNS解析失败，暂不拒绝
        }

        return null;
    }
}