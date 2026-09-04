package com.robot.home.follow.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.common.PageResult;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.follow.entity.Follow;
import com.robot.home.follow.mapper.FollowMapper;
import com.robot.home.follow.service.FollowService;
import com.robot.home.follow.vo.FollowItemVO;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.user.entity.User;
import com.robot.home.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关注服务实现
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private BrandMapper brandMapper;
    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private RobotMapper robotMapper;

    @Override
    public boolean check(Long userId, String followType, Long followId) {
        if (userId == null) {
            return false;
        }
        return count(Wrappers.<Follow>lambdaQuery()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowType, followType)
                .eq(Follow::getFollowId, followId)) > 0;
    }

    @Override
    public Set<Long> checkBatch(Long userId, String followType, List<Long> followIds) {
        if (userId == null || followIds == null || followIds.isEmpty()) {
            return Collections.emptySet();
        }
        return list(Wrappers.<Follow>lambdaQuery()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowType, followType)
                .in(Follow::getFollowId, followIds))
                .stream().map(Follow::getFollowId).collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggle(Long userId, String followType, Long followId) {
        Follow exist = getOne(Wrappers.<Follow>lambdaQuery()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowType, followType)
                .eq(Follow::getFollowId, followId), false);
        if (exist != null) {
            removeById(exist.getId());
            decrFollowCount(followType, followId);
            return false;
        }
        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowType(followType);
        follow.setFollowId(followId);
        save(follow);
        incrFollowCount(followType, followId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, String followType, Long followId) {
        int rows = baseMapper.delete(Wrappers.<Follow>lambdaQuery()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowType, followType)
                .eq(Follow::getFollowId, followId));
        if (rows > 0) {
            decrFollowCount(followType, followId);
        }
    }

    @Override
    public PageResult<FollowItemVO> myFollows(Long userId, String followType, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Follow> page = new Page<>(pn, ps);
        IPage<Follow> result = page(page, Wrappers.<Follow>lambdaQuery()
                .eq(Follow::getUserId, userId)
                .eq(StrUtil.isNotBlank(followType), Follow::getFollowType, followType)
                .orderByDesc(Follow::getCreateTime));

        List<FollowItemVO> items = new ArrayList<>();
        for (Follow f : result.getRecords()) {
            FollowItemVO vo = new FollowItemVO();
            vo.setId(f.getId());
            vo.setFollowType(f.getFollowType());
            vo.setFollowId(f.getFollowId());
            vo.setCreateTime(f.getCreateTime());
            fillTarget(vo, f.getFollowType(), f.getFollowId());
            items.add(vo);
        }
        return PageResult.of(pn, ps, result.getTotal(), items);
    }

    @Override
    public long count(Long userId, String followType) {
        return count(Wrappers.<Follow>lambdaQuery()
                .eq(Follow::getUserId, userId)
                .eq(StrUtil.isNotBlank(followType), Follow::getFollowType, followType));
    }

    private void fillTarget(FollowItemVO vo, String type, Long id) {
        switch (type) {
            case "user": {
                User u = userMapper.selectById(id);
                if (u != null) {
                    vo.setName(u.getNickname() != null ? u.getNickname() : u.getUsername());
                    vo.setAvatar(u.getAvatar());
                    vo.setDescription(u.getIntro());
                    vo.setUrl("/user/" + id);
                }
                break;
            }
            case "brand": {
                Brand b = brandMapper.selectById(id);
                if (b != null) {
                    vo.setName(b.getName());
                    vo.setAvatar(b.getLogo());
                    vo.setDescription(b.getIntro());
                    vo.setUrl("/brand/" + id);
                }
                break;
            }
            case "company": {
                Company c = companyMapper.selectById(id);
                if (c != null) {
                    vo.setName(c.getName());
                    vo.setAvatar(c.getLogo());
                    vo.setDescription(c.getIntro());
                    vo.setUrl("/company/" + id);
                }
                break;
            }
            case "robot": {
                Robot r = robotMapper.selectById(id);
                if (r != null) {
                    vo.setName(r.getName());
                    vo.setAvatar(r.getCoverImage());
                    vo.setDescription(r.getSubtitle());
                    vo.setUrl("/robot/" + id);
                }
                break;
            }
            default:
                break;
        }
    }

    /**
     * 关注数维护：关注用户时，自己 follow_count +1、对方 fans_count +1
     * 品牌 / 企业 / 机器人的粉丝量通过 follow 表实时统计（见 followCount 查询）
     */
    private void incrFollowCount(String type, Long id) {
        updateUserFollowCount(type, id, 1);
    }

    private void decrFollowCount(String type, Long id) {
        updateUserFollowCount(type, id, -1);
    }

    private void updateUserFollowCount(String type, Long targetId, int delta) {
        if (!"user".equals(type) || targetId == null) {
            return;
        }
        Long myId = SecurityUtils.currentUserId();
        if (myId == null) {
            return;
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, targetId)
                .setSql("fans_count = fans_count + (" + delta + ")"));
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, myId)
                .setSql("follow_count = follow_count + (" + delta + ")"));
    }

}
