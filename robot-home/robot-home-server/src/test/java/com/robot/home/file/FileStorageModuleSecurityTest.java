package com.robot.home.file;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

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

    private LocalFileStorageServiceImpl localStorage(Path uploadRoot) {
        LocalFileStorageServiceImpl storage = new LocalFileStorageServiceImpl();
        ReflectionTestUtils.setField(storage, "uploadDir", uploadRoot.toString());
        ReflectionTestUtils.setField(storage, "accessUrl", "/files");
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
