package com.robot.home.robot.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.robot.dto.RobotQuery;
import com.robot.home.robot.entity.RobotImage;
import com.robot.home.robot.entity.RobotVideo;
import com.robot.home.robot.service.RobotService;
import com.robot.home.robot.vo.CompareVO;
import com.robot.home.robot.vo.RelatedArticleVO;
import com.robot.home.robot.vo.RobotDetailVO;
import com.robot.home.robot.vo.RobotFilterVO;
import com.robot.home.robot.vo.RobotListVO;
import com.robot.home.robot.vo.RobotParamGroupVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 机器人库 / 详情 / 参数 / 对比
 */
@RestController
@RequestMapping("/api/robots")
public class RobotController {

    @Resource
    private RobotService robotService;

    /**
     * 机器人库分页筛选
     */
    @GetMapping
    public Result<PageResult<RobotListVO>> page(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long seriesId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String scenes,
            @RequestParam(required = false) String devs,
            @RequestParam(required = false) String ais,
            @RequestParam(required = false) String releaseStart,
            @RequestParam(required = false) String releaseEnd,
            @RequestParam(defaultValue = "comprehensive") String sort,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        RobotQuery query = new RobotQuery();
        query.setCategoryId(categoryId);
        query.setBrandId(brandId);
        query.setSeriesId(seriesId);
        query.setKeyword(keyword);
        query.setMinPrice(minPrice);
        query.setMaxPrice(maxPrice);
        query.setScenes(split(scenes));
        query.setDevs(split(devs));
        query.setAis(split(ais));
        query.setReleaseStart(releaseStart);
        query.setReleaseEnd(releaseEnd);
        query.setSort(sort);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        return Result.success(robotService.page(query, SecurityUtils.currentUserId()));
    }

    /**
     * 筛选项聚合
     */
    @GetMapping("/filters")
    public Result<RobotFilterVO> filters() {
        return Result.success(robotService.filters());
    }

    /**
     * 热门机器人
     */
    @GetMapping("/hot")
    public Result<List<RobotListVO>> hot(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(robotService.hot(limit, SecurityUtils.currentUserId()));
    }

    /**
     * 新品机器人
     */
    @GetMapping("/new")
    public Result<List<RobotListVO>> newest(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(robotService.newest(limit, SecurityUtils.currentUserId()));
    }

    /**
     * 参数对比，最多 4 台：/api/robots/compare?ids=1,2,3
     */
    @GetMapping("/compare")
    public Result<CompareVO> compare(@RequestParam String ids) {
        List<Long> idList = new ArrayList<>();
        for (String part : ids.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                try {
                    idList.add(Long.valueOf(trimmed));
                } catch (NumberFormatException e) {
                    throw new com.robot.home.common.exception.BusinessException("对比参数包含非法 id: " + trimmed);
                }
            }
        }
        CompareVO vo = robotService.compare(idList);
        // 记录对比次数
        robotService.recordCompare(idList);
        return Result.success(vo);
    }

    /**
     * 机器人详情
     */
    @GetMapping("/{id}")
    public Result<RobotDetailVO> detail(@PathVariable Long id) {
        return Result.success(robotService.detail(id, SecurityUtils.currentUserId()));
    }

    /**
     * 参数分组
     */
    @GetMapping("/{id}/params")
    public Result<List<RobotParamGroupVO>> params(@PathVariable Long id) {
        return Result.success(robotService.params(id));
    }

    /**
     * 图片
     */
    @GetMapping("/{id}/images")
    public Result<List<RobotImage>> images(@PathVariable Long id) {
        return Result.success(robotService.images(id));
    }

    /**
     * 视频
     */
    @GetMapping("/{id}/videos")
    public Result<List<RobotVideo>> videos(@PathVariable Long id) {
        return Result.success(robotService.videos(id));
    }

    /**
     * 关联资讯
     */
    @GetMapping("/{id}/articles")
    public Result<List<RelatedArticleVO>> articles(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(robotService.relatedArticles(id, limit));
    }

    /**
     * 上報浏览（用于小程序等需要显式上报的场景）
     */
    @PostMapping("/{id}/view")
    public Result<Void> view(@PathVariable Long id) {
        robotService.recordView(id, SecurityUtils.currentUserId());
        return Result.success();
    }

    private List<String> split(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Arrays.asList(value.split(","));
    }
}
