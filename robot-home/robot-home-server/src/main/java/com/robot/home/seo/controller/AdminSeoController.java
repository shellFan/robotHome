package com.robot.home.seo.controller;

import com.robot.home.common.Result;
import com.robot.home.security.RequirePermission;
import com.robot.home.seo.service.SeoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 后台SEO管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/seo")
public class AdminSeoController {

    @Resource
    private SeoService seoService;

    /**
     * slug批量回填(为缺少slug的实体生成slug)
     */
    @PostMapping("/backfill-slugs")
    @RequirePermission("seo:manage")
    public Result<Map<String, Integer>> backfillSlugs() {
        return Result.success(seoService.backfillSlugs());
    }

    /**
     * 页面访问统计
     */
    @GetMapping("/page-view-stats")
    @RequirePermission("seo:manage")
    public Result<Map<String, Object>> pageViewStats(@RequestParam(defaultValue = "7") int days) {
        if (days < 1) days = 1;
        if (days > 90) days = 90;
        return Result.success(seoService.pageViewStats(days));
    }
}