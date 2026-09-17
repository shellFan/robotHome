package com.robot.home.review.service;

import com.robot.home.common.PageResult;
import com.robot.home.review.dto.ReviewDTO;
import com.robot.home.review.vo.ReviewSummaryVO;
import com.robot.home.review.vo.ReviewVO;

/**
 * 机器人评价服务
 */
public interface RobotReviewService {

    /**
     * 提交评价（每个用户每个机器人仅一条）
     */
    Long submit(Long userId, ReviewDTO dto);

    /**
     * 某机器人的评价列表（仅已通过）
     */
    PageResult<ReviewVO> list(Long robotId, Long currentUserId, Integer pageNum, Integer pageSize);

    /**
     * 某机器人的评价汇总（4维均值+5星分布）
     */
    ReviewSummaryVO summary(Long robotId);

    /**
     * 标记评价有用（防重复投票）
     */
    void helpful(Long userId, Long reviewId);

    /**
     * 取消有用标记
     */
    void unhelpful(Long userId, Long reviewId);

    /**
     * 删除自己的评价（逻辑删除）
     */
    void delete(Long userId, Long reviewId);

    /**
     * 修改自己的评价
     */
    void update(Long userId, Long reviewId, ReviewDTO dto);

    // ---- 管理端 ----

    /**
     * 管理端分页（可按状态/robotId筛选）
     */
    PageResult<ReviewVO> adminPage(Integer status, Long robotId, Integer pageNum, Integer pageSize);

    /**
     * 审核评价（通过/拒绝）
     */
    void audit(Long reviewId, Integer status, String reason);

    /**
     * 官方回复
     */
    void reply(Long reviewId, String content);
}