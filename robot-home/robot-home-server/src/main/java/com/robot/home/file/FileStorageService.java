package com.robot.home.file;

import java.io.InputStream;

/**
 * 文件存储抽象：统一图片 / 视频 / Logo / 头像 / 附件等
 * 实现：LocalFileStorageServiceImpl（本地磁盘，默认）、MinioFileStorageServiceImpl（MinIO）
 * 严禁将文件二进制直接存入 MySQL
 */
public interface FileStorageService {

    /**
     * 上传文件
     *
     * @param data             文件字节
     * @param originalFilename 原始文件名（用于推断扩展名）
     * @param module           业务模块（如 robot / avatar / article），用于分目录
     * @return 可访问的 URL 路径（相对或绝对，由实现决定）
     */
    String upload(byte[] data, String originalFilename, String module);

    /**
     * 上传文件（输入流）
     */
    String upload(InputStream inputStream, String originalFilename, String module);

    /**
     * 删除文件
     *
     * @param url 上传时返回的 URL
     */
    void delete(String url);

    /**
     * 获取访问 URL
     */
    String getAccessUrl(String storageKey);
}
