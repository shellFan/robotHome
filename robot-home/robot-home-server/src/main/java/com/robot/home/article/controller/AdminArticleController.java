package com.robot.home.article.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.article.entity.Article;
import com.robot.home.article.entity.ArticleCategory;
import com.robot.home.article.mapper.ArticleCategoryMapper;
import com.robot.home.article.mapper.ArticleMapper;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.XssUtils;
import com.robot.home.security.RequirePermission;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台资讯管理
 */
@RestController
@RequestMapping("/api/admin/articles")
public class AdminArticleController {

    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private ArticleCategoryMapper categoryMapper;

    @GetMapping
    @RequirePermission("article:list")
    public Result<PageResult<Article>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long categoryId,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Article> page = new Page<>(pn, ps);
        IPage<Article> result = articleMapper.selectPage(page, Wrappers.<Article>lambdaQuery()
                .like(StrUtil.isNotBlank(keyword), Article::getTitle, keyword)
                .eq(categoryId != null, Article::getCategoryId, categoryId)
                .eq(status != null, Article::getStatus, status)
                .orderByDesc(Article::getPublishTime));
        return Result.success(PageResult.of(pn, ps, result.getTotal(), result.getRecords()));
    }

    @GetMapping("/{id}")
    @RequirePermission("article:list")
    public Result<Article> detail(@PathVariable Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("资讯不存在");
        }
        return Result.success(article);
    }

    @PostMapping
    @RequirePermission("article:add")
    public Result<Long> save(@RequestBody Article article) {
        if (StrUtil.isBlank(article.getTitle())) {
            throw new BusinessException("标题不能为空");
        }
        // 富文本正文由后台编辑维护，仍做基础 XSS 清洗
        article.setContent(XssUtils.clean(article.getContent()));
        if (article.getId() == null) {
            if (article.getStatus() == null) {
                article.setStatus(1);
            }
            if (article.getPublishTime() == null) {
                article.setPublishTime(LocalDateTime.now());
            }
            articleMapper.insert(article);
        } else {
            if (articleMapper.selectById(article.getId()) == null) {
                throw new BusinessException("资讯不存在");
            }
            articleMapper.updateById(article);
        }
        return Result.success(article.getId());
    }

    @DeleteMapping("/{id}")
    @RequirePermission("article:delete")
    public Result<Void> delete(@PathVariable Long id) {
        articleMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/{id}/status")
    @RequirePermission("article:publish")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Article article = new Article();
        article.setId(id);
        article.setStatus(status);
        if (Integer.valueOf(1).equals(status)) {
            article.setPublishTime(LocalDateTime.now());
        }
        articleMapper.updateById(article);
        return Result.success();
    }

    @GetMapping("/categories")
    @RequirePermission("article:category")
    public Result<List<ArticleCategory>> categories() {
        return Result.success(categoryMapper.selectList(Wrappers.<ArticleCategory>lambdaQuery()
                .orderByAsc(ArticleCategory::getSort)));
    }

    @PostMapping("/categories")
    @RequirePermission("article:category")
    public Result<Long> saveCategory(@RequestBody ArticleCategory category) {
        if (StrUtil.isBlank(category.getName())) {
            throw new BusinessException("栏目名称不能为空");
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
    @RequirePermission("article:category")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        long count = articleMapper.selectCount(Wrappers.<Article>lambdaQuery().eq(Article::getCategoryId, id));
        if (count > 0) {
            throw new BusinessException("该栏目下仍有 " + count + " 篇文章，不能删除");
        }
        categoryMapper.deleteById(id);
        return Result.success();
    }
}
