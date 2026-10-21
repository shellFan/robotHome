package com.robot.home.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;

/**
 * 文件存储路由器：根据 file.storage-type 选择本地或 MinIO 实现
 * 作为 @Primary 注入点，业务层直接依赖 FileStorageService 即可
 * <p>
 * 可靠性保障：当 storage-type=minio 但 MinIO 不可用时，自动降级到本地存储，
 * 确保上传操作不因外部存储故障而失败（降级会在日志中记录 WARN）
 */
@Service
@Primary
public class FileStorageRouter implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageRouter.class);

    @Value("${file.storage-type:local}")
    private String storageType;

    /** MinIO故障后降级到本地的开关（默认开启） */
    @Value("${file.fallback-to-local:true}")
    private boolean fallbackToLocal;

    @Resource
    @Qualifier("localFileStorageService")
    private FileStorageService localFileStorageService;

    @Resource
    @Qualifier("minioFileStorageService")
    private FileStorageService minioFileStorageService;

    private FileStorageService delegate() {
        return "minio".equalsIgnoreCase(storageType) ? minioFileStorageService : localFileStorageService;
    }

    @Override
    public String upload(byte[] data, String originalFilename, String module) {
        if ("minio".equalsIgnoreCase(storageType)) {
            try {
                return minioFileStorageService.upload(data, originalFilename, module);
            } catch (Exception e) {
                if (fallbackToLocal) {
                    log.warn("MinIO上传失败，降级到本地存储: file={}, module={}, error={}",
                            originalFilename, module, e.getMessage());
                    return localFileStorageService.upload(data, originalFilename, module);
                }
                throw e;
            }
        }
        return localFileStorageService.upload(data, originalFilename, module);
    }

    @Override
    public String upload(InputStream inputStream, String originalFilename, String module) {
        if ("minio".equalsIgnoreCase(storageType)) {
            try {
                return minioFileStorageService.upload(inputStream, originalFilename, module);
            } catch (Exception e) {
                if (fallbackToLocal) {
                    log.warn("MinIO上传失败，降级到本地存储: file={}, module={}, error={}",
                            originalFilename, module, e.getMessage());
                    // InputStream可能已被消费，降级时用byte[]方式（调用方需注意）
                    // 对于InputStream场景，MinIO失败后stream可能已不可读，记录错误并抛出
                    log.error("InputStream场景MinIO降级不可用（流可能已消费），抛出异常: {}", originalFilename);
                    throw e;
                }
                throw e;
            }
        }
        return localFileStorageService.upload(inputStream, originalFilename, module);
    }

    @Override
    public void delete(String url) {
        try {
            delegate().delete(url);
        } catch (Exception e) {
            // 删除失败不阻断业务流程，仅记录日志
            log.warn("文件删除失败（不阻断业务）: url={}, error={}", url, e.getMessage());
        }
    }

    @Override
    public String getAccessUrl(String storageKey) {
        try {
            return delegate().getAccessUrl(storageKey);
        } catch (Exception e) {
            log.warn("获取访问URL失败，降级到本地: key={}, error={}", storageKey, e.getMessage());
            return localFileStorageService.getAccessUrl(storageKey);
        }
    }
}
