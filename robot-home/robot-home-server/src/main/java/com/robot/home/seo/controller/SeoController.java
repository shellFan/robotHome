package com.robot.home.seo.controller;

import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.security.RequirePermission;
import com.robot.home.seo.service.SeoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * SEO Controller
 * 提供sitemap.xml, robots.txt, 页面访问事件上报
 */
@Slf4j
@RestController
@RequestMapping("/api/seo")
public class SeoController {

    @Resource
    private SeoService seoService;

    /**
     * sitemap.xml (无需登录, 供爬虫抓取)
     */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap(HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);
        String xml = seoService.generateSitemap(baseUrl);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE + ";charset=UTF-8")
                .body(xml);
    }

    /**
     * robots.txt (无需登录, 供爬虫抓取)
     */
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robotsTxt(HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);
        String txt = seoService.generateRobotsTxt(baseUrl);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
                .body(txt);
    }

    /**
     * 页面访问事件上报(匿名可调用)
     * 前端在页面加载时上报, 用于增长漏斗分析
     */
    @PostMapping("/track")
    public Result<Void> track(@RequestParam String pageType,
                              @RequestParam(required = false) String pageSlug,
                              @RequestParam(required = false) String pagePath,
                              @RequestParam(required = false) Long entityId,
                              @RequestParam(required = false) String visitorId,
                              @RequestParam(required = false) String refererSource,
                              @RequestParam(required = false) String refererUrl,
                              HttpServletRequest request) {
        // 优先使用登录用户ID作为visitorId
        String vid = visitorId;
        Long userId = SecurityUtils.currentUserId();
        if (userId != null) {
            vid = "u" + userId;
        } else if (vid == null || vid.isEmpty()) {
            // 匿名: 使用IP哈希作为访客指纹
            vid = "a" + hashIp(request.getRemoteAddr());
        }

        String ipHash = hashIp(request.getRemoteAddr());
        String userAgent = request.getHeader("User-Agent");

        seoService.recordPageView(pageType, pageSlug, pagePath, entityId, userId, vid,
                ipHash, userAgent, refererSource, refererUrl);
        return Result.success();
    }

    /**
     * 页面访问统计(管理员/内部)
     */
    @GetMapping("/stats")
    @RequirePermission("seo:manage")
    public Result<Map<String, Object>> stats(@RequestParam(defaultValue = "7") int days) {
        if (days < 1) days = 1;
        if (days > 90) days = 90;
        return Result.success(seoService.pageViewStats(days));
    }

    // ==================== 私有方法 ====================

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        if ((scheme.equals("http") && serverPort != 80)
                || (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        url.append(contextPath);
        return url.toString();
    }

    /**
     * IP哈希(SHA-256前64位), 不存原始IP
     */
    private String hashIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "unknown";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(ip.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            // 截取前64字符
            String full = hex.toString();
            return full.length() > 64 ? full.substring(0, 64) : full;
        } catch (Exception e) {
            return "hash-error";
        }
    }
}
