package com.robot.home.collector.service;

import com.robot.home.collector.fetcher.FetchResult;
import com.robot.home.collector.fetcher.HttpFetcher;
import com.robot.home.collector.util.UrlNormalizer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片处理服务
 * 负责图片下载、格式验证、尺寸检测、存储上传
 */
@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    private static final Set<String> SUPPORTED_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    ));

    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    ));

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB

    @Autowired
    private HttpFetcher httpFetcher;

    @Value("${crawler.image.storage-type:local}")
    private String storageType;

    @Value("${crawler.image.local-path:/tmp/crawler/images}")
    private String localPath;

    /** 已下载URL缓存，避免重复下载 */
    private final Map<String, ImageInfo> downloadedCache = new ConcurrentHashMap<>();

    /**
     * 下载图片
     * @param imageUrl 图片URL
     * @return 图片信息，失败返回null
     */
    public ImageInfo downloadImage(String imageUrl) {
        if (StringUtils.isBlank(imageUrl)) {
            return null;
        }

        String normalizedUrl = UrlNormalizer.normalize(imageUrl);

        // 检查缓存
        ImageInfo cached = downloadedCache.get(normalizedUrl);
        if (cached != null) {
            return cached;
        }

        // 检查URL扩展名
        if (!isImageUrl(normalizedUrl)) {
            log.debug("URL does not look like an image: {}", normalizedUrl);
            // 仍然尝试下载，可能CDN URL无扩展名
        }

        try {
            FetchResult result = httpFetcher.fetchBinary(normalizedUrl);
            if (!result.isSuccess() || result.getBody() == null) {
                log.warn("Failed to download image: {} - status {}", normalizedUrl, result.getStatusCode());
                return null;
            }

            // 检查Content-Type
            String contentType = result.getContentType();
            if (contentType != null && !SUPPORTED_TYPES.contains(contentType.split(";")[0].trim())) {
                log.warn("Unsupported image type: {} for URL {}", contentType, normalizedUrl);
                return null;
            }

            // 检查大小
            if (result.getBody().length > MAX_IMAGE_SIZE) {
                log.warn("Image too large: {} bytes for URL {}", result.getBody().length, normalizedUrl);
                return null;
            }

            ImageInfo info = new ImageInfo();
            info.setUrl(normalizedUrl);
            info.setData(result.getBody());
            info.setContentType(contentType);
            info.setSize(result.getBody().length);
            info.setExtension(guessExtension(normalizedUrl, contentType));

            downloadedCache.put(normalizedUrl, info);
            return info;

        } catch (Exception e) {
            log.error("Error downloading image: {}", normalizedUrl, e);
            return null;
        }
    }

    /**
     * 批量下载图片（去重+限速）
     * @param imageUrls 图片URL列表
     * @return 成功下载的图片信息列表
     */
    public List<ImageInfo> downloadImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return Collections.emptyList();
        }

        List<ImageInfo> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String url : imageUrls) {
            String normalized = UrlNormalizer.normalize(url);
            if (seen.contains(normalized)) continue;
            seen.add(normalized);

            ImageInfo info = downloadImage(normalized);
            if (info != null) {
                results.add(info);
            }
        }

        return results;
    }

    /**
     * 判断URL是否为图片URL
     */
    public boolean isImageUrl(String url) {
        if (StringUtils.isBlank(url)) return false;
        String lower = url.toLowerCase();

        // 检查扩展名
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lower.endsWith("." + ext) || lower.contains("." + ext + "?")) {
                return true;
            }
        }

        // 检查常见CDN图片路径
        if (lower.contains("/image") || lower.contains("/img") || lower.contains("/photo") ||
            lower.contains("/pic") || lower.contains("/thumb") || lower.contains("/avatar")) {
            return true;
        }

        return false;
    }

    /**
     * 推断图片扩展名
     */
    private String guessExtension(String url, String contentType) {
        // 从URL推断
        String path = UrlNormalizer.getPath(url);
        if (path != null) {
            int dotIdx = path.lastIndexOf('.');
            if (dotIdx > 0 && dotIdx < path.length() - 1) {
                String ext = path.substring(dotIdx + 1).toLowerCase();
                if (SUPPORTED_EXTENSIONS.contains(ext.split("[?#]")[0])) {
                    return ext.split("[?#]")[0];
                }
            }
        }

        // 从Content-Type推断
        if (contentType != null) {
            String ct = contentType.split(";")[0].trim().toLowerCase();
            switch (ct) {
                case "image/jpeg": return "jpg";
                case "image/png": return "png";
                case "image/gif": return "gif";
                case "image/webp": return "webp";
            }
        }

        return "jpg"; // 默认
    }

    /**
     * 清理下载缓存
     */
    public void clearCache() {
        downloadedCache.clear();
    }

    /**
     * 图片信息
     */
    public static class ImageInfo {
        private String url;
        private byte[] data;
        private String contentType;
        private long size;
        private String extension;
        private String storagePath; // 存储路径（上传后设置）

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public byte[] getData() { return data; }
        public void setData(byte[] data) { this.data = data; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public String getExtension() { return extension; }
        public void setExtension(String extension) { this.extension = extension; }
        public String getStoragePath() { return storagePath; }
        public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    }
}