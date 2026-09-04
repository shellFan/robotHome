package com.robot.home.article.service;

import com.robot.home.article.vo.ArticleDetailVO;
import com.robot.home.article.vo.ArticleListVO;
import com.robot.home.common.PageResult;

import java.util.List;

/**
 * 资讯服务
 */
public interface ArticleService {

    PageResult<ArticleListVO> page(Long categoryId, String keyword, Integer pageNum, Integer pageSize);

    ArticleDetailVO detail(Long id, Long currentUserId);

    List<ArticleListVO> hot(int limit);

    /**
     * 栏目列表（含文章数）
     */
    List<com.robot.home.article.vo.CategoryCountVO> categories();
}
