package com.robot.home.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 本地磁盘文件存储实现（默认）
 * 文件写入 upload-dir，通过 /files/** 静态资源映射对外访问
 */
@Service("localFileStorageService")
public class LocalFileStorageServiceImpl implements FileStorageService {

    @Value("${file.local.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.local.access-url:/files}")
    private String accessUrl;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public String upload(byte[] data, String originalFilename, String module) {
        String ext = ext(originalFilename);
        String relativeDir = StrUtil.join("/", module, LocalDate.now().format(DF));
        File dir = new File(uploadDir, relativeDir);
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
        return normalize(accessUrl + "/" + relativeDir + "/" + fileName);
    }

    @Override
    public String upload(InputStream inputStream, String originalFilename, String module) {
        String ext = ext(originalFilename);
        String relativeDir = StrUtil.join("/", module, LocalDate.now().format(DF));
        File dir = new File(uploadDir, relativeDir);
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
        return normalize(accessUrl + "/" + relativeDir + "/" + fileName);
    }

    @Override
    public void delete(String url) {
        if (StrUtil.isBlank(url)) {
            return;
        }
        String key = url.startsWith(accessUrl) ? url.substring(accessUrl.length()) : url;
        File file = new File(uploadDir, key);
        if (file.exists()) {
            FileUtil.del(file);
        }
    }

    @Override
    public String getAccessUrl(String storageKey) {
        if (StrUtil.isBlank(storageKey)) {
            return "";
        }
        if (storageKey.startsWith("http") || storageKey.startsWith(accessUrl)) {
            return storageKey;
        }
        return normalize(accessUrl + "/" + storageKey);
    }

    private String ext(String originalFilename) {
        if (StrUtil.isBlank(originalFilename) || !originalFilename.contains(".")) {
            return ".bin";
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
    }

    private String normalize(String path) {
        return path.replace("\\", "/");
    }
}
