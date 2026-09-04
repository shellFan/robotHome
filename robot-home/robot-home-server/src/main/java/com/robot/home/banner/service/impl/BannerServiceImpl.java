package com.robot.home.banner.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.banner.entity.Banner;
import com.robot.home.banner.mapper.BannerMapper;
import com.robot.home.banner.service.BannerService;
import com.robot.home.common.Constants;
import com.robot.home.common.util.RedisUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Banner 服务实现：结果缓存到 Redis，后台修改后需清理缓存
 */
@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {

    private static final long CACHE_SECONDS = 600L;

    @Resource
    private RedisUtils redisUtils;

    @Override
    public List<Banner> list(String position) {
        String key = Constants.CACHE_BANNER_PREFIX + position;
        String cached = redisUtils.get(key);
        if (cached != null) {
            try {
                List<Banner> list = JSONUtil.toList(JSONUtil.parseArray(cached), Banner.class);
                if (list != null) {
                    return list;
                }
            } catch (Exception ignored) {
                // 缓存解析失败时回源数据库
            }
        }
        List<Banner> list = list(Wrappers.<Banner>lambdaQuery()
                .eq(Banner::getPosition, position)
                .eq(Banner::getStatus, 1)
                .orderByAsc(Banner::getSort));
        if (list == null) {
            list = new ArrayList<>();
        }
        redisUtils.set(key, JSONUtil.toJsonStr(list), CACHE_SECONDS, TimeUnit.SECONDS);
        return list;
    }
}
