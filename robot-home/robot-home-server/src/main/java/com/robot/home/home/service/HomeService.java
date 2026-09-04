package com.robot.home.home.service;

import com.robot.home.home.vo.HomeIndexVO;

/**
 * 首页聚合服务
 */
public interface HomeService {

    /**
     * 首页数据聚合
     *
     * @param position pc / app
     */
    HomeIndexVO index(String position);
}
