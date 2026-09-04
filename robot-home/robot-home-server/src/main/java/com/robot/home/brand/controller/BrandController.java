package com.robot.home.brand.controller;

import com.robot.home.brand.service.BrandService;
import com.robot.home.brand.vo.BrandDetailVO;
import com.robot.home.brand.vo.BrandLetterGroupVO;
import com.robot.home.brand.vo.BrandListVO;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.robot.vo.RobotSummaryVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 品牌库
 */
@RestController
@RequestMapping("/api/brands")
public class BrandController {

    @Resource
    private BrandService brandService;

    @GetMapping
    public Result<PageResult<BrandListVO>> page(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String initial,
                                                @RequestParam(required = false) Boolean hot,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(brandService.page(keyword, initial, hot, pageNum, pageSize));
    }

    @GetMapping("/letters")
    public Result<List<BrandLetterGroupVO>> letters() {
        return Result.success(brandService.groupByLetter());
    }

    @GetMapping("/hot")
    public Result<List<BrandListVO>> hot(@RequestParam(defaultValue = "12") Integer limit) {
        return Result.success(brandService.hot(limit));
    }

    @GetMapping("/{id}")
    public Result<BrandDetailVO> detail(@PathVariable Long id) {
        return Result.success(brandService.detail(id));
    }

    @GetMapping("/{id}/robots")
    public Result<PageResult<RobotSummaryVO>> robots(@PathVariable Long id,
                                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                                     @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(brandService.robots(id, pageNum, pageSize));
    }
}
