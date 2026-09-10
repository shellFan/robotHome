package com.robot.home.crawler;

import com.robot.home.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Crawler 网关代理：Admin Browser → JWT → Server → API Key → Collector
 *
 * 将 /api/admin/crawler/** 请求转发到 Collector /api/crawler/**，
 * 内部添加 X-API-Key 请求头，隐藏 API Key 不暴露给浏览器。
 *
 * 安全设计：
 * 1. 模块白名单：仅允许 source/task/content/publish 四个模块
 * 2. 路径校验：禁止路径遍历（..）
 * 3. 目标地址固定：从配置读取，客户端不可控
 * 4. 错误透传：Collector 返回 4xx/5xx 时合理映射，不全部变 500
 * 5. Collector 不可用：返回明确错误信息
 */
@RestController
@RequestMapping("/api/admin/crawler")
public class CrawlerProxyController {

    private static final Logger log = LoggerFactory.getLogger(CrawlerProxyController.class);

    /** 允许代理的模块白名单，防止万能代理 */
    private static final Set<String> ALLOWED_MODULES = new HashSet<>(
            Arrays.asList("source", "task", "content", "publish"));

    private final RestTemplate restTemplate;

    @Value("${collector.url:http://localhost:8082}")
    private String collectorUrl;

    @Value("${collector.api-key:}")
    private String collectorApiKey;

    public CrawlerProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 通用代理：捕获 /api/admin/crawler/** 下所有子路径
     * 将 /api/admin/crawler/{module}/** 转发为 /api/crawler/{module}/**
     */
    @RequestMapping(value = "/{module}/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public Result<Object> proxy(
            @PathVariable String module,
            HttpServletRequest request) throws IOException {

        // 1. 模块白名单校验 — 禁止万能代理
        if (!ALLOWED_MODULES.contains(module)) {
            log.warn("Proxy rejected: module '{}' not in whitelist", module);
            return Result.error(404, "接口不存在: " + module);
        }

        // 2. 路径遍历校验
        String servletPath = request.getServletPath();
        if (servletPath.contains("..")) {
            log.warn("Proxy rejected: path traversal attempt in '{}'", servletPath);
            return Result.error(400, "非法请求路径");
        }

        // 3. 构建目标路径
        String targetPath = servletPath.replace("/api/admin/crawler", "/api/crawler");

        // 4. 构建目标URL（含查询参数）
        StringBuilder urlBuilder = new StringBuilder(collectorUrl).append(targetPath);
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            urlBuilder.append('?').append(queryString);
        }
        String targetUrl = urlBuilder.toString();

        log.info("Proxy: {} {} → {}", request.getMethod(), servletPath, targetUrl);

        // 5. 构建请求头：转发 Content-Type，添加 X-API-Key
        HttpHeaders headers = new HttpHeaders();
        String contentType = request.getContentType();
        if (contentType != null) {
            headers.setContentType(MediaType.parseMediaType(contentType));
        }
        if (collectorApiKey != null && !collectorApiKey.isEmpty()) {
            headers.set("X-API-Key", collectorApiKey);
        }

        // 6. 读取请求体（GET/DELETE 无请求体）
        byte[] body = null;
        if (hasBody(request)) {
            body = StreamUtils.copyToByteArray(request.getInputStream());
        }

        // 7. 构建请求实体
        HttpEntity<byte[]> requestEntity = new HttpEntity<>(body, headers);

        // 8. 发起请求 — 带错误透传
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        try {
            ResponseEntity<Object> responseEntity = restTemplate.exchange(
                    URI.create(targetUrl), method, requestEntity, Object.class);
            Object data = responseEntity.getBody();
            return Result.success(data);

        } catch (HttpClientErrorException e) {
            // Collector 返回 4xx — 透传状态码和消息
            log.warn("Collector returned {}: {} - {}", e.getStatusCode(), servletPath, e.getResponseBodyAsString());
            return Result.error(e.getStatusCode().value(), "采集服务请求错误: " + e.getStatusText());

        } catch (HttpServerErrorException e) {
            // Collector 返回 5xx — 透传但不暴露内部详情
            log.error("Collector server error {}: {} - {}", e.getStatusCode(), servletPath, e.getResponseBodyAsString());
            return Result.error(502, "采集服务内部错误");

        } catch (ResourceAccessException e) {
            // Collector 不可用（连接超时/拒绝连接）
            log.error("Collector unavailable: {} - {}", servletPath, e.getMessage());
            return Result.error(503, "采集服务不可用，请稍后重试");

        } catch (Exception e) {
            // 其他异常
            log.error("Proxy error: {} - {}", servletPath, e.getMessage(), e);
            return Result.error(500, "网关代理错误");
        }
    }

    /**
     * 判断请求是否有请求体
     */
    private boolean hasBody(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }
}