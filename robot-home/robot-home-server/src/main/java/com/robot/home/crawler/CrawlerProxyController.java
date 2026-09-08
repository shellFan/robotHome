package com.robot.home.crawler;

import com.robot.home.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;

/**
 * Crawler 网关代理：Admin Browser → JWT → Server → API Key → Collector
 *
 * 将 /api/admin/crawler/** 请求转发到 Collector /api/crawler/**，
 * 内部添加 X-API-Key 请求头，隐藏 API Key 不暴露给浏览器。
 *
 * 安全链路：JwtFilter → AdminInterceptor → CrawlerProxyController → Collector
 */
@RestController
@RequestMapping("/api/admin/crawler")
public class CrawlerProxyController {

    private static final Logger log = LoggerFactory.getLogger(CrawlerProxyController.class);

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
     * 将 /api/admin/crawler/xxx 转发为 /api/crawler/xxx
     */
    @RequestMapping(value = "/{module}/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public Result<Object> proxy(
            @PathVariable String module,
            HttpServletRequest request) throws IOException {

        // 构建目标路径：/api/crawler/{module}/子路径
        String servletPath = request.getServletPath(); // /api/admin/crawler/source/list
        String targetPath = servletPath.replace("/api/admin/crawler", "/api/crawler"); // /api/crawler/source/list

        // 构建目标URL（含查询参数）
        StringBuilder urlBuilder = new StringBuilder(collectorUrl).append(targetPath);
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            urlBuilder.append('?').append(queryString);
        }
        String targetUrl = urlBuilder.toString();

        log.debug("Proxy: {} {} → {}", request.getMethod(), servletPath, targetUrl);

        // 构建请求头：转发 Content-Type，添加 X-API-Key
        HttpHeaders headers = new HttpHeaders();
        String contentType = request.getContentType();
        if (contentType != null) {
            headers.setContentType(MediaType.parseMediaType(contentType));
        }
        if (collectorApiKey != null && !collectorApiKey.isEmpty()) {
            headers.set("X-API-Key", collectorApiKey);
        }

        // 读取请求体（GET/DELETE 无请求体）
        byte[] body = null;
        if (hasBody(request)) {
            body = StreamUtils.copyToByteArray(request.getInputStream());
        }

        // 构建请求实体
        HttpEntity<byte[]> requestEntity = new HttpEntity<>(body, headers);

        // 发起请求
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        ResponseEntity<Object> responseEntity = restTemplate.exchange(
                URI.create(targetUrl), method, requestEntity, Object.class);

        // 返回统一格式
        Object data = responseEntity.getBody();
        return Result.success(data);
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