package com.robot.home.similar.service;

import com.robot.home.similar.vo.SimilarRobotVO;

import java.util.List;

/**
 * 相似机器人推荐服务
 */
public interface RobotSimilarService {

    /**
     * 获取某机器人的相似推荐列表
     */
    List<SimilarRobotVO> listSimilar(Long robotId, Integer limit);

    /**
     * 重新计算某机器人的相似度评分（管理端触发）
     */
    void recalculate(Long robotId);

    /**
     * 全量重算所有机器人的相似度评分（管理端触发）
     */
    void recalculateAll();
}