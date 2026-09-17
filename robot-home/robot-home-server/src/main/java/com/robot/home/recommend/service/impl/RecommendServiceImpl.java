package com.robot.home.recommend.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.common.Constants;
import com.robot.home.common.util.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.robot.home.recommend.entity.RecommendItem;
import com.robot.home.recommend.entity.RecommendPosition;
import com.robot.home.recommend.mapper.RecommendItemMapper;
import com.robot.home.recommend.mapper.RecommendPositionMapper;
import com.robot.home.recommend.service.RecommendService;
import com.robot.home.recommend.vo.RecommendItemVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 推荐位服务实现
 */
@Service
public class RecommendServiceImpl extends ServiceImpl<RecommendItemMapper, RecommendItem> implements RecommendService {

    private static final Logger log = LoggerFactory.getLogger(RecommendServiceImpl.class);

    private static final long CACHE_SECONDS = 600L;

    @Resource
    private RecommendPositionMapper positionMapper;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public List<RecommendItemVO> items(String code) {
        if (StrUtil.isBlank(code)) {
            return new ArrayList<>();
        }
        String key = Constants.CACHE_RECOMMEND_PREFIX + code;
        try {
            String cached = redisUtils.get(key);
            if (cached != null) {
                try {
                    List<RecommendItemVO> list = JSONUtil.toList(JSONUtil.parseArray(cached), RecommendItemVO.class);
                    if (list != null) {
                        return list;
                    }
                } catch (Exception ignored) {
                    // 缓存解析失败时回源数据库
                }
            }
        } catch (Exception e) {
            log.warn("Redis推荐位缓存读取失败，降级到DB查询: code={}, error={}", code, e.getMessage());
        }
        RecommendPosition position = positionMapper.selectOne(Wrappers.<RecommendPosition>lambdaQuery()
                .eq(RecommendPosition::getCode, code)
                .eq(RecommendPosition::getStatus, 1)
                .last("LIMIT 1"));
        List<RecommendItemVO> vos = new ArrayList<>();
        if (position != null) {
            List<RecommendItem> items = list(Wrappers.<RecommendItem>lambdaQuery()
                    .eq(RecommendItem::getPositionId, position.getId())
                    .eq(RecommendItem::getStatus, 1)
                    .orderByAsc(RecommendItem::getSort));
            vos = items.stream().map(i -> {
                RecommendItemVO vo = new RecommendItemVO();
                vo.setId(i.getId());
                vo.setBizType(i.getBizType());
                vo.setBizId(i.getBizId());
                vo.setTitle(i.getTitle());
                vo.setImage(i.getImage());
                vo.setUrl(i.getUrl());
                vo.setSort(i.getSort());
                return vo;
            }).collect(Collectors.toList());
        }
        try {
            redisUtils.set(key, JSONUtil.toJsonStr(vos), CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis推荐位缓存写入失败（不影响返回）: code={}, error={}", code, e.getMessage());
        }
        return vos;
    }
}
