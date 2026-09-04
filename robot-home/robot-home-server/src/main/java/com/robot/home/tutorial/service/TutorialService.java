package com.robot.home.tutorial.service;

import com.robot.home.common.PageResult;
import com.robot.home.tutorial.vo.TutorialDetailVO;
import com.robot.home.tutorial.vo.TutorialListVO;

import java.util.List;

/**
 * 教程服务
 */
public interface TutorialService {

    PageResult<TutorialListVO> page(Long categoryId, String keyword, Integer pageNum, Integer pageSize);

    TutorialDetailVO detail(Long id, Long currentUserId);

    List<TutorialListVO> hot(int limit);

    List<com.robot.home.article.vo.CategoryCountVO> categories();
}
