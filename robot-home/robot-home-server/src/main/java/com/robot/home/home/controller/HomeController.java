package com.robot.home.home.controller;

import com.robot.home.common.Result;
import com.robot.home.home.service.HomeService;
import com.robot.home.home.vo.HomeIndexVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 首页聚合接口（PC / 小程序共用）
 */
@RestController
@RequestMapping("/api/home")
public class HomeController {

    @Resource
    private HomeService homeService;

    @GetMapping("/index")
    public Result<HomeIndexVO> index(@RequestParam(defaultValue = "pc") String position) {
        return Result.success(homeService.index(position));
    }
}
