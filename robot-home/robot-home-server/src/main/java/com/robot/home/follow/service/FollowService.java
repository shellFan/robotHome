package com.robot.home.follow.service;

import com.robot.home.common.PageResult;
import com.robot.home.follow.vo.FollowItemVO;

import java.util.List;

/**
 * 关注服务：用户 / 品牌 / 企业 / 机器人
 */
public interface FollowService {

    boolean check(Long userId, String followType, Long followId);

    /**
     * 切换关注状态，返回切换后是否关注
     */
    boolean toggle(Long userId, String followType, Long followId);

    void remove(Long userId, String followType, Long followId);

    PageResult<FollowItemVO> myFollows(Long userId, String followType, Integer pageNum, Integer pageSize);

    long count(Long userId, String followType);

    /**
     * 批量判断关注状态，返回已关注的 id 集合
     */
    java.util.Set<Long> checkBatch(Long userId, String followType, List<Long> followIds);
}
