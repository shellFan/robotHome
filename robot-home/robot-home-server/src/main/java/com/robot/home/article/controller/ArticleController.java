package com.robot.home.article.controller;

import com.robot.home.article.service.ArticleService;
import com.robot.home.article.vo.ArticleDetailVO;
import com.robot.home.article.vo.ArticleListVO;
import com.robot.home.article.vo.CategoryCountVO;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 资讯
 */
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Resource
    private ArticleService articleService;

    @GetMapping
    public Result<PageResult<ArticleListVO>> page(@RequestParam(required = false) Long categoryId,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(articleService.page(categoryId, keyword, pageNum, pageSize));
    }

    @GetMapping("/categories")
    public Result<List<CategoryCountVO>> categories() {
        return Result.success(articleService.categories());
    }

    @GetMapping("/hot")
    public Result<List<ArticleListVO>> hot(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(articleService.hot(limit));
    }

    @GetMapping("/{id}")
    public Result<ArticleDetailVO> detail(@PathVariable Long id) {
        return Result.success(articleService.detail(id, SecurityUtils.currentUserId()));
    }
}
