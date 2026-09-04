package com.robot.home.recommend.service;

import com.robot.home.recommend.vo.RecommendItemVO;

import java.util.List;

/**
 * 推荐位服务
 */
public interface RecommendService {

    /**
     * 按推荐位编码获取内容（带缓存）
     */
    List<RecommendItemVO> items(String code);
}
