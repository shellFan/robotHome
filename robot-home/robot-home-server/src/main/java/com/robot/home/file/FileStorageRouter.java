package com.robot.home.file;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;

/**
 * 文件存储路由器：根据 file.storage-type 选择本地或 MinIO 实现
 * 作为 @Primary 注入点，业务层直接依赖 FileStorageService 即可
 */
@Service
@Primary
public class FileStorageRouter implements FileStorageService {

    @Value("${file.storage-type:local}")
    private String storageType;

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
        return delegate().upload(data, originalFilename, module);
    }

    @Override
    public String upload(InputStream inputStream, String originalFilename, String module) {
        return delegate().upload(inputStream, originalFilename, module);
    }

    @Override
    public void delete(String url) {
        delegate().delete(url);
    }

    @Override
    public String getAccessUrl(String storageKey) {
        return delegate().getAccessUrl(storageKey);
    }
}
