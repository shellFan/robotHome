package com.robot.home.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 本地磁盘文件存储实现（默认）
 * 公开文件写入 upload-dir，通过 /files/** 静态资源映射对外访问
 * 私有文件写入 private-upload-dir，不对外公开，须通过鉴权端点访问
 */
@Service("localFileStorageService")
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageServiceImpl.class);

    @Value("${file.local.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.local.access-url:/files}")
    private String accessUrl;

    /** 私有文件上传目录（不通过 /files/ 公开暴露） */
    @Value("${file.local.private-upload-dir:./private-uploads}")
    private String privateUploadDir;

    /** 私有文件访问前缀（须通过鉴权端点） */
    @Value("${file.local.private-access-url:/api/files/private}")
    private String privateAccessUrl;

    /** 允许的文件扩展名白名单（防御层：FileController 已有白名单，此处兜底） */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg",
            "mp4", "webm", "mov", "avi", "mkv",
            "pdf", "doc", "docx", "xls", "xlsx", "zip", "bin"
    ));

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public String upload(byte[] data, String originalFilename, String module) {
        String normalizedModule = FileModulePolicy.normalize(module);
        boolean isPrivate = FileModulePolicy.isPrivateModule(normalizedModule);
        String baseDir = isPrivate ? privateUploadDir : uploadDir;
        String baseUrl = isPrivate ? privateAccessUrl : accessUrl;

        String ext = ext(originalFilename);
        String relativeDir = StrUtil.join("/", normalizedModule, LocalDate.now().format(DF));
        File dir = new File(baseDir, relativeDir);
        if (!dir.exists()) {
            FileUtil.mkdir(dir);
        }
        String fileName = IdUtil.fastSimpleUUID() + ext;
        File target = new File(dir, fileName);
        try {
            FileUtil.writeBytes(data, target);
        } catch (Exception e) {
            throw new RuntimeException("文件写入失败: " + e.getMessage(), e);
        }
        if (isPrivate) {
            log.info("私有文件上传: module={}, file={}", normalizedModule, fileName);
        }
        return normalize(baseUrl + "/" + relativeDir + "/" + fileName);
    }

    @Override
    public String upload(InputStream inputStream, String originalFilename, String module) {
        String normalizedModule = FileModulePolicy.normalize(module);
        boolean isPrivate = FileModulePolicy.isPrivateModule(normalizedModule);
        String baseDir = isPrivate ? privateUploadDir : uploadDir;
        String baseUrl = isPrivate ? privateAccessUrl : accessUrl;

        String ext = ext(originalFilename);
        String relativeDir = StrUtil.join("/", normalizedModule, LocalDate.now().format(DF));
        File dir = new File(baseDir, relativeDir);
        if (!dir.exists()) {
            FileUtil.mkdir(dir);
        }
        String fileName = IdUtil.fastSimpleUUID() + ext;
        File target = new File(dir, fileName);
        try {
            FileUtil.writeFromStream(inputStream, target);
        } catch (Exception e) {
            throw new RuntimeException("文件写入失败: " + e.getMessage(), e);
        }
        return normalize(baseUrl + "/" + relativeDir + "/" + fileName);
    }

    @Override
    public void delete(String url) {
        if (StrUtil.isBlank(url)) {
            return;
        }
        // 判断是公开还是私有文件
        boolean isPrivate = url.startsWith(privateAccessUrl);
        String baseDir = isPrivate ? privateUploadDir : uploadDir;
        String baseUrl = isPrivate ? privateAccessUrl : accessUrl;

        String key = url.startsWith(baseUrl) ? url.substring(baseUrl.length()) : url;
        // 路径穿越防护：确保删除目标在基础目录内
        File base = new File(baseDir).getAbsoluteFile();
        File target = new File(base, key.startsWith("/") ? key.substring(1) : key);
        try {
            String targetCanonical = target.getCanonicalPath();
            String baseCanonical = base.getCanonicalPath();
            if (!targetCanonical.startsWith(baseCanonical + File.separator) && !targetCanonical.equals(baseCanonical)) {
                log.warn("路径穿越攻击拦截: url={}, resolved={}, base={}", url, targetCanonical, baseCanonical);
                return;
            }
        } catch (IOException e) {
            log.warn("路径校验失败，拒绝删除: {}", url);
            return;
        }
        if (target.exists()) {
            FileUtil.del(target);
        }
    }

    @Override
    public String getAccessUrl(String storageKey) {
        if (StrUtil.isBlank(storageKey)) {
            return "";
        }
        if (storageKey.startsWith("http") || storageKey.startsWith(accessUrl) || storageKey.startsWith(privateAccessUrl)) {
            return storageKey;
        }
        return normalize(accessUrl + "/" + storageKey);
    }

    /**
     * 解析私有文件 URL 为本地文件（供鉴权下载端点使用）
     * @return 对应的本地 File，不存在或路径穿越返回 null
     */
    public File resolvePrivateFile(String privateUrl) {
        if (StrUtil.isBlank(privateUrl) || !privateUrl.startsWith(privateAccessUrl)) {
            return null;
        }
        String key = privateUrl.substring(privateAccessUrl.length());
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        File base = new File(privateUploadDir).getAbsoluteFile();
        File target = new File(base, key);
        try {
            String targetCanonical = target.getCanonicalPath();
            String baseCanonical = base.getCanonicalPath();
            if (!targetCanonical.startsWith(baseCanonical + File.separator)) {
                log.warn("私有文件路径穿越拦截: url={}", privateUrl);
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        return target.exists() ? target : null;
    }

    private String ext(String originalFilename) {
        if (StrUtil.isBlank(originalFilename) || !originalFilename.contains(".")) {
            return ".bin";
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT);
        // 防御层：拒绝危险扩展名（FileController 已有白名单，此处兜底）
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            log.warn("拒绝危险扩展名: .{} (filename={}), 降级为 .bin", ext, originalFilename);
            return ".bin";
        }
        return "." + ext;
    }

    private String normalize(String path) {
        return path.replace("\\", "/");
    }
}