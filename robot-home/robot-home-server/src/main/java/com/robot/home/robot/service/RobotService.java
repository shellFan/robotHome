package com.robot.home.robot.service;

import com.robot.home.common.PageResult;
import com.robot.home.robot.dto.RobotQuery;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.entity.RobotImage;
import com.robot.home.robot.entity.RobotVideo;
import com.robot.home.robot.vo.CompareVO;
import com.robot.home.robot.vo.RelatedArticleVO;
import com.robot.home.robot.vo.RobotDetailVO;
import com.robot.home.robot.vo.RobotFilterVO;
import com.robot.home.robot.vo.RobotListVO;
import com.robot.home.robot.vo.RobotParamGroupVO;

import java.util.List;

/**
 * 机器人服务
 */
public interface RobotService {

    /**
     * 机器人库分页筛选
     */
    PageResult<RobotListVO> page(RobotQuery query, Long currentUserId);

    /**
     * 机器人详情（并记录浏览）
     */
    RobotDetailVO detail(Long id, Long currentUserId);

    /**
     * 参数分组（含取值）
     */
    List<RobotParamGroupVO> params(Long id);

    List<RobotImage> images(Long id);

    List<RobotVideo> videos(Long id);

    /**
     * 关联资讯
     */
    List<RelatedArticleVO> relatedArticles(Long id, int limit);

    /**
     * 参数对比（最多 4 台）
     */
    CompareVO compare(List<Long> ids);

    /**
     * 筛选项聚合
     */
    RobotFilterVO filters();

    List<RobotListVO> hot(int limit, Long currentUserId);

    List<RobotListVO> newest(int limit, Long currentUserId);

    /**
     * 记录浏览（浏览量 +1、写入浏览历史）
     */
    void recordView(Long id, Long userId);

    /**
     * 对比数 +1
     */
    void recordCompare(List<Long> ids);

    /**
     * 后台：按 id 查询原始数据
     */
    Robot getByIdOrNull(Long id);
}
