package com.robot.home.file;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * MinIO 文件存储实现（可切换）
 * <p>
 * 私有模块隔离：私有模块（企业认证材料等）objectName 加 private/ 前缀，
 * 返回鉴权端点 URL（/api/files/private?url=minio://bucket/objectName），
 * 不直接暴露 endpoint 公开 URL。
 */
@Service("minioFileStorageService")
public class MinioFileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioFileStorageServiceImpl.class);

    @Value("${file.minio.endpoint}")
    private String endpoint;

    @Value("${file.minio.access-key}")
    private String accessKey;

    @Value("${file.minio.secret-key}")
    private String secretKey;

    @Value("${file.minio.bucket-name}")
    private String bucketName;

    /** 私有文件访问前缀（与 LocalFileStorageServiceImpl 保持一致） */
    @Value("${file.local.private-access-url:/api/files/private}")
    private String privateAccessUrl;

    /** 私有对象前缀（MinIO 内隔离私有模块文件） */
    private static final String PRIVATE_OBJECT_PREFIX = "private/";

    /** MinIO 私有文件 URL 标识 scheme */
    private static final String PRIVATE_URL_SCHEME = "minio://";

    /** 允许的文件扩展名白名单（防御层：FileController 已有白名单，此处兜底，与本地实现一致） */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg",
            "mp4", "webm", "mov", "avi", "mkv",
            "pdf", "doc", "docx", "xls", "xlsx", "zip", "bin"
    ));

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    private String ext(String originalFilename) {
        if (StrUtil.isBlank(originalFilename) || !originalFilename.contains(".")) {
            return ".bin";
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT);
        // 防御层：拒绝危险扩展名（FileController 已有白名单，此处兜底）
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            log.warn("MinIO拒绝危险扩展名: .{} (filename={}), 降级为 .bin", ext, originalFilename);
            return ".bin";
        }
        return "." + ext;
    }

    @Override
    public String upload(byte[] data, String originalFilename, String module) {
        String normalizedModule = FileModulePolicy.normalize(module);
        boolean isPrivate = FileModulePolicy.isPrivateModule(normalizedModule);
        String objectName = buildObjectName(normalizedModule, isPrivate, originalFilename);
        try (InputStream is = new ByteArrayInputStream(data)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(is, data.length, -1)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO 上传失败: " + e.getMessage(), e);
        }
        return buildResultUrl(objectName, isPrivate);
    }

    @Override
    public String upload(InputStream inputStream, String originalFilename, String module) {
        String normalizedModule = FileModulePolicy.normalize(module);
        boolean isPrivate = FileModulePolicy.isPrivateModule(normalizedModule);
        String objectName = buildObjectName(normalizedModule, isPrivate, originalFilename);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, -1, 10485760)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO 上传失败: " + e.getMessage(), e);
        }
        return buildResultUrl(objectName, isPrivate);
    }

    /** 构造对象名：私有模块加 private/ 前缀实现隔离 */
    private String buildObjectName(String normalizedModule, boolean isPrivate, String originalFilename) {
        String prefix = isPrivate ? PRIVATE_OBJECT_PREFIX : "";
        return prefix + normalizedModule + "/" + IdUtil.fastSimpleUUID() + ext(originalFilename);
    }

    /** 构造返回 URL：私有文件走鉴权端点，公开文件直接 endpoint */
    private String buildResultUrl(String objectName, boolean isPrivate) {
        if (isPrivate) {
            log.info("MinIO私有文件上传: object={}", objectName);
            return privateAccessUrl + "?url=" + PRIVATE_URL_SCHEME + bucketName + "/" + objectName;
        }
        return endpoint + "/" + bucketName + "/" + objectName;
    }

    @Override
    public void delete(String url) {
        if (StrUtil.isBlank(url)) {
            return;
        }
        String objectName = extractObjectName(url);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO 删除失败: " + e.getMessage(), e);
        }
    }

    /** 从 URL 中提取对象名（兼容公开 URL、私有鉴权 URL、裸对象名） */
    private String extractObjectName(String url) {
        // 私有鉴权 URL: /api/files/private?url=minio://bucket/object
        int queryIdx = url.indexOf("url=" + PRIVATE_URL_SCHEME);
        if (queryIdx >= 0) {
            String minioUri = url.substring(queryIdx + 4);
            String path = minioUri.substring(PRIVATE_URL_SCHEME.length());
            int slash = path.indexOf('/');
            return slash > 0 ? path.substring(slash + 1) : path;
        }
        // 公开 URL: endpoint/bucket/object
        String prefix = endpoint + "/" + bucketName + "/";
        if (url.startsWith(prefix)) {
            return url.substring(prefix.length());
        }
        // 裸对象名
        return url;
    }

    @Override
    public String getAccessUrl(String storageKey) {
        if (StrUtil.isBlank(storageKey)) {
            return "";
        }
        if (storageKey.startsWith("http") || storageKey.startsWith(privateAccessUrl)) {
            return storageKey;
        }
        return endpoint + "/" + bucketName + "/" + storageKey;
    }

    /**
     * 解析 MinIO 私有文件 URL 为输入流（供鉴权下载端点使用）
     * @param minioUrl minio://bucket/objectName 格式
     * @return 文件输入流，不存在或非法返回 null
     */
    public InputStream resolvePrivateObject(String minioUrl) {
        if (StrUtil.isBlank(minioUrl) || !minioUrl.startsWith(PRIVATE_URL_SCHEME)) {
            return null;
        }
        String path = minioUrl.substring(PRIVATE_URL_SCHEME.length());
        int slash = path.indexOf('/');
        if (slash <= 0) {
            return null;
        }
        String bucket = path.substring(0, slash);
        String objectName = path.substring(slash + 1);
        // 只允许访问私有前缀的对象，防止通过此端点读取公开对象绕过审计
        if (!objectName.startsWith(PRIVATE_OBJECT_PREFIX)) {
            log.warn("MinIO私有文件访问拦截: 非私有对象 {}", minioUrl);
            return null;
        }
        if (!bucketName.equals(bucket)) {
            log.warn("MinIO私有文件访问拦截: 非配置bucket {}", minioUrl);
            return null;
        }
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO私有文件获取失败: {}, error={}", minioUrl, e.getMessage());
            return null;
        }
    }
}
