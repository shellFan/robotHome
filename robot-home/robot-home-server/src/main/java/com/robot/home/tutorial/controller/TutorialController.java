package com.robot.home.tutorial.controller;

import com.robot.home.article.vo.CategoryCountVO;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.tutorial.service.TutorialService;
import com.robot.home.tutorial.vo.TutorialDetailVO;
import com.robot.home.tutorial.vo.TutorialListVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 教程
 */
@RestController
@RequestMapping("/api/tutorials")
public class TutorialController {

    @Resource
    private TutorialService tutorialService;

    @GetMapping
    public Result<PageResult<TutorialListVO>> page(@RequestParam(required = false) Long categoryId,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(tutorialService.page(categoryId, keyword, pageNum, pageSize));
    }

    @GetMapping("/categories")
    public Result<List<CategoryCountVO>> categories() {
        return Result.success(tutorialService.categories());
    }

    @GetMapping("/hot")
    public Result<List<TutorialListVO>> hot(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(tutorialService.hot(limit));
    }

    @GetMapping("/{id}")
    public Result<TutorialDetailVO> detail(@PathVariable Long id) {
        return Result.success(tutorialService.detail(id, SecurityUtils.currentUserId()));
    }
}
