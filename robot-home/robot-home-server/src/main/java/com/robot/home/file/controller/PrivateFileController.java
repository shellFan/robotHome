package com.robot.home.file.controller;

import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.file.LocalFileStorageServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

/**
 * 私有文件鉴权下载端点
 *
 * 私有文件（企业认证材料、营业执照、采购合同等）不通过公开 /files/ 暴露，
 * 必须通过此端点访问，且要求用户已登录。
 * 文件路径穿越防护由 LocalFileStorageServiceImpl.resolvePrivateFile 保证。
 */
@RestController
@RequestMapping("/api/files")
public class PrivateFileController {

    private static final Logger log = LoggerFactory.getLogger(PrivateFileController.class);

    @Autowired
    private LocalFileStorageServiceImpl localFileStorageService;

    /**
     * 私有文件下载（须登录）
     * @param url 私有文件URL（以 /api/files/private/ 开头）
     */
    @GetMapping("/private")
    public ResponseEntity<Resource> downloadPrivate(@RequestParam("url") String url) {
        // 必须登录才能访问私有文件
        Long userId = SecurityUtils.requireUserId();
        if (url == null || url.trim().isEmpty()) {
            throw new BusinessException("缺少文件地址");
        }
        File file = localFileStorageService.resolvePrivateFile(url.trim());
        if (file == null || !file.exists()) {
            log.warn("私有文件不存在或路径非法: userId={}, url={}", userId, url);
            throw new BusinessException("文件不存在或无权访问");
        }
        Resource resource = new FileSystemResource(file);
        String contentType = guessContentType(file.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private String guessContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "application/msword";
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return "application/vnd.ms-excel";
        if (lower.endsWith(".zip")) return "application/zip";
        if (lower.endsWith(".mp4")) return "video/mp4";
        return "application/octet-stream";
    }
}