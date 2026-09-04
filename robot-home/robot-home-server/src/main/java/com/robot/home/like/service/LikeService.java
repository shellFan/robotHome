package com.robot.home.like.service;

import java.util.List;
import java.util.Set;

/**
 * 通用点赞服务
 */
public interface LikeService {

    boolean check(Long userId, String bizType, Long bizId);

    Set<Long> checkBatch(Long userId, String bizType, List<Long> bizIds);

    /**
     * 切换点赞状态，返回切换后是否点赞
     */
    boolean toggle(Long userId, String bizType, Long bizId);

    long count(Long userId);
}
