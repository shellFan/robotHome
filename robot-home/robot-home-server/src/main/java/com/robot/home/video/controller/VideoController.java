package com.robot.home.video.controller;

import com.robot.home.article.vo.CategoryCountVO;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.video.service.VideoService;
import com.robot.home.video.vo.VideoDetailVO;
import com.robot.home.video.vo.VideoListVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 视频
 */
@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @Resource
    private VideoService videoService;

    @GetMapping
    public Result<PageResult<VideoListVO>> page(@RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(videoService.page(categoryId, keyword, pageNum, pageSize));
    }

    @GetMapping("/categories")
    public Result<List<CategoryCountVO>> categories() {
        return Result.success(videoService.categories());
    }

    @GetMapping("/hot")
    public Result<List<VideoListVO>> hot(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(videoService.hot(limit));
    }

    @GetMapping("/{id}")
    public Result<VideoDetailVO> detail(@PathVariable Long id) {
        return Result.success(videoService.detail(id, SecurityUtils.currentUserId()));
    }
}
