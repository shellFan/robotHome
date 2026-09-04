package com.robot.home.recommend.controller;

import com.robot.home.common.Result;
import com.robot.home.recommend.service.RecommendService;
import com.robot.home.recommend.vo.RecommendItemVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 推荐位
 */
@RestController
@RequestMapping("/api/recommends")
public class RecommendController {

    @Resource
    private RecommendService recommendService;

    @GetMapping("/{code}")
    public Result<List<RecommendItemVO>> items(@PathVariable String code) {
        return Result.success(recommendService.items(code));
    }
}
