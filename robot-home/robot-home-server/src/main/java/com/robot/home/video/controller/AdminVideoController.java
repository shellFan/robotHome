package com.robot.home.video.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.security.RequirePermission;
import com.robot.home.video.entity.Video;
import com.robot.home.video.entity.VideoCategory;
import com.robot.home.video.mapper.VideoCategoryMapper;
import com.robot.home.video.mapper.VideoMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台视频管理
 */
@RestController
@RequestMapping("/api/admin/videos")
public class AdminVideoController {

    @Resource
    private VideoMapper videoMapper;
    @Resource
    private VideoCategoryMapper categoryMapper;

    @GetMapping
    @RequirePermission("video:list")
    public Result<PageResult<Video>> page(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "20") Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Video> page = new Page<>(pn, ps);
        IPage<Video> result = videoMapper.selectPage(page, Wrappers.<Video>lambdaQuery()
                .like(StrUtil.isNotBlank(keyword), Video::getTitle, keyword)
                .eq(categoryId != null, Video::getCategoryId, categoryId)
                .eq(status != null, Video::getStatus, status)
                .orderByDesc(Video::getPublishTime));
        return Result.success(PageResult.of(pn, ps, result.getTotal(), result.getRecords()));
    }

    @GetMapping("/{id}")
    @RequirePermission("video:list")
    public Result<Video> detail(@PathVariable Long id) {
        Video video = videoMapper.selectById(id);
        if (video == null) {
            throw new BusinessException("视频不存在");
        }
        return Result.success(video);
    }

    @PostMapping
    @RequirePermission("video:add")
    public Result<Long> save(@RequestBody Video video) {
        if (StrUtil.isBlank(video.getTitle())) {
            throw new BusinessException("标题不能为空");
        }
        if (StrUtil.isBlank(video.getUrl())) {
            throw new BusinessException("视频地址不能为空");
        }
        if (video.getId() == null) {
            if (video.getStatus() == null) {
                video.setStatus(1);
            }
            if (video.getPublishTime() == null) {
                video.setPublishTime(LocalDateTime.now());
            }
            videoMapper.insert(video);
        } else {
            if (videoMapper.selectById(video.getId()) == null) {
                throw new BusinessException("视频不存在");
            }
            videoMapper.updateById(video);
        }
        return Result.success(video.getId());
    }

    @DeleteMapping("/{id}")
    @RequirePermission("video:delete")
    public Result<Void> delete(@PathVariable Long id) {
        videoMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/{id}/status")
    @RequirePermission("video:edit")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Video video = new Video();
        video.setId(id);
        video.setStatus(status);
        videoMapper.updateById(video);
        return Result.success();
    }

    @GetMapping("/categories")
    @RequirePermission("video:category")
    public Result<List<VideoCategory>> categories() {
        return Result.success(categoryMapper.selectList(Wrappers.<VideoCategory>lambdaQuery()
                .orderByAsc(VideoCategory::getSort)));
    }

    @PostMapping("/categories")
    @RequirePermission("video:category")
    public Result<Long> saveCategory(@RequestBody VideoCategory category) {
        if (StrUtil.isBlank(category.getName())) {
            throw new BusinessException("频道名称不能为空");
        }
        if (category.getId() == null) {
            if (category.getStatus() == null) {
                category.setStatus(1);
            }
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
        return Result.success(category.getId());
    }

    @DeleteMapping("/categories/{id}")
    @RequirePermission("video:category")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        long count = videoMapper.selectCount(Wrappers.<Video>lambdaQuery().eq(Video::getCategoryId, id));
        if (count > 0) {
            throw new BusinessException("该频道下仍有 " + count + " 个视频，不能删除");
        }
        categoryMapper.deleteById(id);
        return Result.success();
    }
}
