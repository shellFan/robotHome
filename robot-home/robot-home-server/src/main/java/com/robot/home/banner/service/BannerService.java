package com.robot.home.banner.service;

import com.robot.home.banner.entity.Banner;

import java.util.List;

/**
 * Banner 服务
 */
public interface BannerService {

    /**
     * 按位置获取生效中的 Banner（带缓存）
     */
    List<Banner> list(String position);
}
