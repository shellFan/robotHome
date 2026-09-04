package com.robot.home.like.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.common.service.BizCounter;
import com.robot.home.like.entity.UserLike;
import com.robot.home.like.mapper.UserLikeMapper;
import com.robot.home.like.service.LikeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通用点赞服务实现
 */
@Service
public class LikeServiceImpl extends ServiceImpl<UserLikeMapper, UserLike> implements LikeService {

    @Resource
    private BizCounter bizCounter;

    @Override
    public boolean check(Long userId, String bizType, Long bizId) {
        if (userId == null) {
            return false;
        }
        return count(Wrappers.<UserLike>lambdaQuery()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getBizType, bizType)
                .eq(UserLike::getBizId, bizId)) > 0;
    }

    @Override
    public Set<Long> checkBatch(Long userId, String bizType, List<Long> bizIds) {
        if (userId == null || bizIds == null || bizIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<UserLike> list = list(Wrappers.<UserLike>lambdaQuery()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getBizType, bizType)
                .in(UserLike::getBizId, bizIds));
        return list.stream().map(UserLike::getBizId).collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggle(Long userId, String bizType, Long bizId) {
        UserLike exist = getOne(Wrappers.<UserLike>lambdaQuery()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getBizType, bizType)
                .eq(UserLike::getBizId, bizId), false);
        if (exist != null) {
            removeById(exist.getId());
            bizCounter.decr(bizType, bizId, BizCounter.Field.LIKE);
            return false;
        }
        UserLike like = new UserLike();
        like.setUserId(userId);
        like.setBizType(bizType);
        like.setBizId(bizId);
        save(like);
        bizCounter.incr(bizType, bizId, BizCounter.Field.LIKE);
        return true;
    }

    @Override
    public long count(Long userId) {
        return count(Wrappers.<UserLike>lambdaQuery().eq(UserLike::getUserId, userId));
    }
}
