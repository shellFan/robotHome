package com.robot.home.video.service;

import com.robot.home.common.PageResult;
import com.robot.home.video.vo.VideoDetailVO;
import com.robot.home.video.vo.VideoListVO;

import java.util.List;

/**
 * 视频服务
 */
public interface VideoService {

    PageResult<VideoListVO> page(Long categoryId, String keyword, Integer pageNum, Integer pageSize);

    VideoDetailVO detail(Long id, Long currentUserId);

    List<VideoListVO> hot(int limit);

    List<com.robot.home.article.vo.CategoryCountVO> categories();
}
