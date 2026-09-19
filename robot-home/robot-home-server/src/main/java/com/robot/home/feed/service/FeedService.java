package com.robot.home.feed.service;

import com.robot.home.common.PageResult;
import com.robot.home.feed.vo.FeedItemVO;

/**
 * Phase9: 关注动态Feed服务
 * 查询式 + Redis Cache
 */
public interface FeedService {

    /**
     * 获取当前用户的关注动态
     *
     * @param userId   当前用户ID
     * @param lastId   上一页最后一条ID（cursor分页，首次传null）
     * @param pageSize 每页数量
     * @return Feed列表
     */
    PageResult<FeedItemVO> myFeed(Long userId, Long lastId, Integer pageSize);
}