package com.robot.home.file.controller;

import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.file.FileStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 文件上传：图片 / 视频 / 头像 / 附件统一入口
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    /** 允许的扩展名白名单 */
    private static final List<String> ALLOWED = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg",
            "mp4", "webm", "mov", "avi", "mkv",
            "pdf", "doc", "docx", "xls", "xlsx", "zip");

    /** 单文件最大 20MB */
    private static final long MAX_SIZE = 20 * 1024 * 1024L;

    @Resource
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(defaultValue = "common") String module) {
        SecurityUtils.requireUserId();
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("文件大小不能超过 20MB");
        }
        String original = file.getOriginalFilename();
        String ext = extension(original);
        if (!ALLOWED.contains(ext)) {
            throw new BusinessException("不支持的文件类型: " + ext);
        }
        String url;
        try {
            url = fileStorageService.upload(file.getBytes(), original, module);
        } catch (Exception e) {
            throw new BusinessException("文件上传失败，请稍后重试");
        }
        Map<String, Object> data = new HashMap<>(4);
        data.put("url", url);
        data.put("size", file.getSize());
        data.put("name", original);
        return Result.success(data);
    }

    private String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
