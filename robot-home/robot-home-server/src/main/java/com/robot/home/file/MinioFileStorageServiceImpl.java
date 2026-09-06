package com.robot.home.file;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * MinIO 文件存储实现（可切换）
 */
@Service("minioFileStorageService")
public class MinioFileStorageServiceImpl implements FileStorageService {

    @Value("${file.minio.endpoint}")
    private String endpoint;

    @Value("${file.minio.access-key}")
    private String accessKey;

    @Value("${file.minio.secret-key}")
    private String secretKey;

    @Value("${file.minio.bucket-name}")
    private String bucketName;

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
        return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
    }

    @Override
    public String upload(byte[] data, String originalFilename, String module) {
        String objectName = module + "/" + IdUtil.fastSimpleUUID() + ext(originalFilename);
        try (InputStream is = new ByteArrayInputStream(data)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(is, data.length, -1)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO 上传失败: " + e.getMessage(), e);
        }
        return endpoint + "/" + bucketName + "/" + objectName;
    }

    @Override
    public String upload(InputStream inputStream, String originalFilename, String module) {
        String objectName = module + "/" + IdUtil.fastSimpleUUID() + ext(originalFilename);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, -1, 10485760)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO 上传失败: " + e.getMessage(), e);
        }
        return endpoint + "/" + bucketName + "/" + objectName;
    }

    @Override
    public void delete(String url) {
        if (StrUtil.isBlank(url)) {
            return;
        }
        String prefix = endpoint + "/" + bucketName + "/";
        String objectName = url.startsWith(prefix) ? url.substring(prefix.length()) : url;
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO 删除失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getAccessUrl(String storageKey) {
        if (StrUtil.isBlank(storageKey)) {
            return "";
        }
        if (storageKey.startsWith("http")) {
            return storageKey;
        }
        return endpoint + "/" + bucketName + "/" + storageKey;
    }
}
