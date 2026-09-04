package com.robot.home.favorite.service;

import com.robot.home.common.PageResult;
import com.robot.home.favorite.vo.FavoriteItemVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 统一收藏服务：robot / article / video / post / tutorial 共用一套逻辑
 */
public interface FavoriteService {

    /**
     * 是否已收藏
     */
    boolean check(Long userId, String bizType, Long bizId);

    /**
     * 批量判断收藏状态，返回已收藏的 bizId 集合
     */
    Set<Long> checkBatch(Long userId, String bizType, List<Long> bizIds);

    /**
     * 切换收藏状态，返回切换后是否收藏
     */
    boolean toggle(Long userId, String bizType, Long bizId);

    /**
     * 取消收藏
     */
    void remove(Long userId, String bizType, Long bizId);

    /**
     * 我的收藏（分页，回填标题与图片）
     */
    PageResult<FavoriteItemVO> myFavorites(Long userId, String bizType, Integer pageNum, Integer pageSize);

    /**
     * 收藏数
     */
    long count(Long userId, String bizType);

    /**
     * 按业务 id 统计收藏数（用于列表展示）
     */
    Map<Long, Long> countByBizIds(String bizType, List<Long> bizIds);
}
