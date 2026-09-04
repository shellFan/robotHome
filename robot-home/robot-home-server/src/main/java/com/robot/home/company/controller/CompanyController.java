package com.robot.home.company.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.company.service.CompanyService;
import com.robot.home.company.vo.CompanyDetailVO;
import com.robot.home.company.vo.CompanyListVO;
import com.robot.home.robot.vo.RobotSummaryVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 企业库
 */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    @Resource
    private CompanyService companyService;

    @GetMapping
    public Result<PageResult<CompanyListVO>> page(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String region,
                                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(companyService.page(keyword, region, pageNum, pageSize));
    }

    @GetMapping("/regions")
    public Result<List<String>> regions() {
        return Result.success(companyService.regions());
    }

    @GetMapping("/hot")
    public Result<List<CompanyListVO>> hot(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(companyService.hot(limit));
    }

    @GetMapping("/{id}")
    public Result<CompanyDetailVO> detail(@PathVariable Long id) {
        return Result.success(companyService.detail(id));
    }

    @GetMapping("/{id}/robots")
    public Result<PageResult<RobotSummaryVO>> robots(@PathVariable Long id,
                                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                                     @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(companyService.robots(id, pageNum, pageSize));
    }
}
