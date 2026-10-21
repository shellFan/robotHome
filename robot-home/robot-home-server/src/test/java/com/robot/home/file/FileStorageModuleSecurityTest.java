package com.robot.home.file;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class FileStorageModuleSecurityTest {

    @TempDir
    Path tempDir;

    @Test
    void localStorageRejectsTraversalBeforeWritingOutsideUploadRoot() throws Exception {
        Path uploadRoot = Files.createDirectory(tempDir.resolve("uploads"));
        Path escapedRoot = tempDir.resolve("escaped");
        LocalFileStorageServiceImpl storage = localStorage(uploadRoot);

        assertThrows(IllegalArgumentException.class,
                () -> storage.upload("attack".getBytes(StandardCharsets.UTF_8), "proof.txt", "../escaped"));

        assertFalse(Files.exists(escapedRoot));
    }

    @Test
    void minioStorageRejectsTraversalBeforeCreatingObject() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        MinioFileStorageServiceImpl storage = minioStorage(minioClient);

        assertThrows(IllegalArgumentException.class,
                () -> storage.upload("attack".getBytes(StandardCharsets.UTF_8), "proof.txt", "../escaped"));

        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
    }

    @Test
    void currentModulesRemainAllowedAndAreNormalized() {
        List<String> modules = Arrays.asList(
                "common", "article", "brand", "company", "community", "video",
                "tutorial", "recommend", "banner", "robot", "admin", "avatar");
        LocalFileStorageServiceImpl storage = localStorage(tempDir.resolve("uploads"));

        for (String module : modules) {
            String url = storage.upload(new byte[]{1}, "image.jpg", "  " + module.toUpperCase() + "  ");
            assertTrue(url.startsWith("/files/" + module + "/"), "module should be normalized: " + module);
        }
    }

    // ==================== NIGHT3: 私有文件隔离测试 ====================

    @Test
    void enterpriseModuleUploadGoesToPrivateDir() throws Exception {
        Path uploadRoot = Files.createDirectory(tempDir.resolve("uploads"));
        Path privateRoot = Files.createDirectory(tempDir.resolve("private-uploads"));
        LocalFileStorageServiceImpl storage = localStorage(uploadRoot, privateRoot);

        String url = storage.upload("cert-data".getBytes(StandardCharsets.UTF_8), "license.jpg", "enterprise");

        // URL 应以私有前缀开头，而非公开 /files/
        assertTrue(url.startsWith("/api/files/private/enterprise/"), "private URL prefix: " + url);
        // 文件应写入 private-uploads 目录
        File privateFile = storage.resolvePrivateFile(url);
        assertTrue(privateFile != null && privateFile.exists(), "private file should exist in private dir");
        // 确认不在公开目录
        assertFalse(url.startsWith("/files/"), "private file must not be publicly accessible");
    }

    @Test
    void publicModuleStillGoesToPublicDir() throws Exception {
        Path uploadRoot = Files.createDirectory(tempDir.resolve("uploads"));
        Path privateRoot = Files.createDirectory(tempDir.resolve("private-uploads"));
        LocalFileStorageServiceImpl storage = localStorage(uploadRoot, privateRoot);

        String url = storage.upload("img".getBytes(StandardCharsets.UTF_8), "photo.jpg", "robot");

        assertTrue(url.startsWith("/files/robot/"), "public URL prefix: " + url);
    }

    @Test
    void deleteRejectsPathTraversalOutsideUploadRoot() throws Exception {
        Path uploadRoot = Files.createDirectory(tempDir.resolve("uploads"));
        Path privateRoot = Files.createDirectory(tempDir.resolve("private-uploads"));
        LocalFileStorageServiceImpl storage = localStorage(uploadRoot, privateRoot);

        // 在uploads外创建一个敏感文件
        Path secretFile = tempDir.resolve("secret.txt");
        Files.write(secretFile, "secret".getBytes(StandardCharsets.UTF_8));

        // 尝试通过delete路径穿越删除uploads外的文件
        storage.delete("/files/../../secret.txt");

        assertTrue(Files.exists(secretFile), "path traversal delete must be blocked");
    }

    @Test
    void resolvePrivateFileRejectsPathTraversal() throws Exception {
        Path uploadRoot = Files.createDirectory(tempDir.resolve("uploads"));
        Path privateRoot = Files.createDirectory(tempDir.resolve("private-uploads"));
        LocalFileStorageServiceImpl storage = localStorage(uploadRoot, privateRoot);

        // 在private-uploads外创建文件
        Path outsideFile = tempDir.resolve("outside.txt");
        Files.write(outsideFile, "outside".getBytes(StandardCharsets.UTF_8));

        // 路径穿越URL
        File result = storage.resolvePrivateFile("/api/files/private/../../outside.txt");
        assertTrue(result == null, "path traversal in resolvePrivateFile must return null");
    }

    @Test
    void resolvePrivateFileRejectsNonPrivateUrl() throws Exception {
        Path uploadRoot = Files.createDirectory(tempDir.resolve("uploads"));
        LocalFileStorageServiceImpl storage = localStorage(uploadRoot);

        // 公开URL不应通过私有解析
        File result = storage.resolvePrivateFile("/files/robot/test.jpg");
        assertTrue(result == null, "public URL must not resolve via private endpoint");
    }

    @Test
    void dangerousExtensionDowngradedToBin() throws Exception {
        Path uploadRoot = Files.createDirectory(tempDir.resolve("uploads"));
        LocalFileStorageServiceImpl storage = localStorage(uploadRoot);

        // .jsp 是危险扩展名，应降级为 .bin
        String url = storage.upload("evil".getBytes(StandardCharsets.UTF_8), "shell.jsp", "robot");
        assertTrue(url.endsWith(".bin"), "dangerous extension must be downgraded: " + url);

        // .exe 同样降级
        String url2 = storage.upload("evil".getBytes(StandardCharsets.UTF_8), "trojan.exe", "robot");
        assertTrue(url2.endsWith(".bin"), ".exe must be downgraded: " + url2);
    }

    @Test
    void safeExtensionPreserved() throws Exception {
        Path uploadRoot = Files.createDirectory(tempDir.resolve("uploads"));
        LocalFileStorageServiceImpl storage = localStorage(uploadRoot);

        String url = storage.upload("img".getBytes(StandardCharsets.UTF_8), "photo.png", "robot");
        assertTrue(url.endsWith(".png"), "safe extension preserved: " + url);
    }

    @Test
    void deletePrivateFileWorksAndStaysInPrivateDir() throws Exception {
        Path uploadRoot = Files.createDirectory(tempDir.resolve("uploads"));
        Path privateRoot = Files.createDirectory(tempDir.resolve("private-uploads"));
        LocalFileStorageServiceImpl storage = localStorage(uploadRoot, privateRoot);

        String url = storage.upload("cert".getBytes(StandardCharsets.UTF_8), "license.pdf", "enterprise");
        File privateFile = storage.resolvePrivateFile(url);
        assertTrue(privateFile != null && privateFile.exists(), "file should exist before delete");

        storage.delete(url);
        assertFalse(privateFile.exists(), "private file should be deleted");
    }

    private LocalFileStorageServiceImpl localStorage(Path uploadRoot) {
        return localStorage(uploadRoot, uploadRoot.getParent().resolve("private-uploads"));
    }

    private LocalFileStorageServiceImpl localStorage(Path uploadRoot, Path privateRoot) {
        LocalFileStorageServiceImpl storage = new LocalFileStorageServiceImpl();
        ReflectionTestUtils.setField(storage, "uploadDir", uploadRoot.toString());
        ReflectionTestUtils.setField(storage, "accessUrl", "/files");
        ReflectionTestUtils.setField(storage, "privateUploadDir", privateRoot.toString());
        ReflectionTestUtils.setField(storage, "privateAccessUrl", "/api/files/private");
        return storage;
    }

    private MinioFileStorageServiceImpl minioStorage(MinioClient minioClient) {
        MinioFileStorageServiceImpl storage = new MinioFileStorageServiceImpl();
        ReflectionTestUtils.setField(storage, "endpoint", "http://minio.test");
        ReflectionTestUtils.setField(storage, "bucketName", "uploads");
        ReflectionTestUtils.setField(storage, "minioClient", minioClient);
        return storage;
    }
}
