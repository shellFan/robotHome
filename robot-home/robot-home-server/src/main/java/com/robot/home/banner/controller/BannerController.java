package com.robot.home.banner.controller;

import com.robot.home.banner.entity.Banner;
import com.robot.home.banner.service.BannerService;
import com.robot.home.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * Banner
 */
@RestController
@RequestMapping("/api/banners")
public class BannerController {

    @Resource
    private BannerService bannerService;

    /**
     * @param position pc / app
     */
    @GetMapping
    public Result<List<Banner>> list(@RequestParam(defaultValue = "pc") String position) {
        return Result.success(bannerService.list(position));
    }
}
