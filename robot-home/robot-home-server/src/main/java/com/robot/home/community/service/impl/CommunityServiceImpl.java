package com.robot.home.community.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.exception.PermissionException;
import com.robot.home.common.service.BizCounter;
import com.robot.home.common.util.JsonUtils;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.XssUtils;
import com.robot.home.community.dto.PostDTO;
import com.robot.home.community.entity.CommunityCircle;
import com.robot.home.community.entity.CommunityPost;
import com.robot.home.community.mapper.CommunityCircleMapper;
import com.robot.home.community.mapper.CommunityPostMapper;
import com.robot.home.community.service.CommunityService;
import com.robot.home.community.vo.CircleVO;
import com.robot.home.community.vo.PostAuthorVO;
import com.robot.home.community.vo.PostVO;
import com.robot.home.favorite.service.FavoriteService;
import com.robot.home.like.service.LikeService;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.user.entity.User;
import com.robot.home.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 社区服务实现
 */
@Service
public class CommunityServiceImpl extends ServiceImpl<CommunityPostMapper, CommunityPost> implements CommunityService {

    @Resource
    private CommunityCircleMapper circleMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private RobotMapper robotMapper;
    @Resource
    private BrandMapper brandMapper;
    @Resource
    private LikeService likeService;
    @Resource
    private FavoriteService favoriteService;
    @Resource
    private BizCounter bizCounter;

    @Override
    public List<CircleVO> circles(Long currentUserId) {
        List<CommunityCircle> circles = circleMapper.selectList(Wrappers.<CommunityCircle>lambdaQuery()
                .eq(CommunityCircle::getStatus, 1)
                .orderByAsc(CommunityCircle::getSort));
        List<CircleVO> vos = new ArrayList<>();
        for (CommunityCircle c : circles) {
            CircleVO vo = new CircleVO();
            vo.setId(c.getId());
            vo.setName(c.getName());
            vo.setLogo(c.getLogo());
            vo.setDescription(c.getDescription());
            vo.setFollowCount(c.getFollowCount());
            vo.setSort(c.getSort());
            // 帖子数实时统计，避免冗余字段与真实数据不一致
            long count = count(Wrappers.<CommunityPost>lambdaQuery()
                    .eq(CommunityPost::getCircleId, c.getId())
                    .eq(CommunityPost::getStatus, 1));
            vo.setPostCount((int) count);
            vo.setFollowed(false);
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public PageResult<PostVO> posts(Long circleId, String topic, String keyword, String sort,
                                    Long currentUserId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<CommunityPost> page = new Page<>(pn, ps);
        IPage<CommunityPost> result = page(page, Wrappers.<CommunityPost>lambdaQuery()
                .eq(CommunityPost::getStatus, 1)
                .eq(circleId != null, CommunityPost::getCircleId, circleId)
                .eq(StrUtil.isNotBlank(topic), CommunityPost::getTopic, topic)
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .like(CommunityPost::getTitle, keyword)
                        .or().like(CommunityPost::getContent, keyword)
                        .or().like(CommunityPost::getTopic, keyword))
                .orderByDesc(CommunityPost::getIsTop)
                .orderBy("hot".equals(sort), false, CommunityPost::getLikeCount)
                .orderByDesc(CommunityPost::getCreateTime));
        return toPageResult(result, currentUserId, pn, ps);
    }

    @Override
    public PostVO detail(Long id, Long currentUserId) {
        CommunityPost post = getById(id);
        if (post == null || !Integer.valueOf(1).equals(post.getStatus())) {
            throw new BusinessException("帖子不存在或已下架");
        }
        PostVO vo = toVO(post, currentUserId);
        bizCounter.incr("post", id, BizCounter.Field.VIEW);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, PostDTO dto) {
        if (StrUtil.isBlank(dto.getContent()) && StrUtil.isBlank(dto.getTitle())) {
            throw new BusinessException("标题和正文不能同时为空");
        }
        if (dto.getCircleId() != null) {
            CommunityCircle circle = circleMapper.selectById(dto.getCircleId());
            if (circle == null) {
                throw new BusinessException("圈子不存在");
            }
        }
        CommunityPost post = new CommunityPost();
        post.setUserId(userId);
        post.setCircleId(dto.getCircleId());
        post.setTitle(StrUtil.isBlank(dto.getTitle()) ? null : XssUtils.clean(StrUtil.trim(dto.getTitle())));
        post.setContent(XssUtils.clean(dto.getContent()));
        post.setImages(JsonUtils.writeStringList(dto.getImages()));
        post.setVideoUrl(dto.getVideoUrl());
        post.setRobotId(dto.getRobotId());
        post.setBrandId(dto.getBrandId());
        post.setTopic(StrUtil.isBlank(dto.getTopic()) ? null : StrUtil.trim(dto.getTopic()));
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setFavoriteCount(0);
        post.setViewCount(0);
        post.setIsTop(0);
        post.setStatus(1);
        save(post);

        // 用户发帖数 +1
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("post_count = post_count + 1"));
        return post.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long postId, boolean isAdmin) {
        CommunityPost post = getById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!isAdmin && !post.getUserId().equals(userId)) {
            throw new PermissionException("只能删除自己的帖子");
        }
        removeById(postId);
    }

    @Override
    public PageResult<PostVO> myPosts(Long userId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<CommunityPost> page = new Page<>(pn, ps);
        IPage<CommunityPost> result = page(page, Wrappers.<CommunityPost>lambdaQuery()
                .eq(CommunityPost::getUserId, userId)
                .orderByDesc(CommunityPost::getCreateTime));
        return toPageResult(result, userId, pn, ps);
    }

    @Override
    public List<String> hotTopics(int limit) {
        List<CommunityPost> posts = list(Wrappers.<CommunityPost>lambdaQuery()
                .eq(CommunityPost::getStatus, 1)
                .isNotNull(CommunityPost::getTopic)
                .ne(CommunityPost::getTopic, "")
                .orderByDesc(CommunityPost::getViewCount)
                .last("LIMIT 200"));
        Set<String> set = new LinkedHashSet<>();
        for (CommunityPost p : posts) {
            if (StrUtil.isNotBlank(p.getTopic())) {
                set.add(p.getTopic());
            }
            if (set.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(set);
    }

    @Override
    public PageResult<PostVO> postsByTarget(String targetType, Long targetId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<CommunityPost> page = new Page<>(pn, ps);
        IPage<CommunityPost> result = page(page, Wrappers.<CommunityPost>lambdaQuery()
                .eq(CommunityPost::getStatus, 1)
                .eq("robot".equals(targetType), CommunityPost::getRobotId, targetId)
                .eq("brand".equals(targetType), CommunityPost::getBrandId, targetId)
                .orderByDesc(CommunityPost::getCreateTime));
        return toPageResult(result, null, pn, ps);
    }

    private PageResult<PostVO> toPageResult(IPage<CommunityPost> result, Long currentUserId, int pn, int ps) {
        List<PostVO> vos = result.getRecords().stream()
                .map(p -> toVO(p, currentUserId))
                .collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), vos);
    }

    private PostVO toVO(CommunityPost p, Long currentUserId) {
        PostVO vo = new PostVO();
        vo.setId(p.getId());
        vo.setCircleId(p.getCircleId());
        vo.setTitle(p.getTitle());
        vo.setContent(p.getContent());
        vo.setImages(JsonUtils.parseStringList(p.getImages()));
        vo.setVideoUrl(p.getVideoUrl());
        vo.setRobotId(p.getRobotId());
        vo.setBrandId(p.getBrandId());
        vo.setTopic(p.getTopic());
        vo.setLikeCount(p.getLikeCount());
        vo.setCommentCount(p.getCommentCount());
        vo.setFavoriteCount(p.getFavoriteCount());
        vo.setViewCount(p.getViewCount());
        vo.setIsTop(p.getIsTop());
        vo.setStatus(p.getStatus());
        vo.setCreateTime(p.getCreateTime());

        if (p.getCircleId() != null) {
            CommunityCircle circle = circleMapper.selectById(p.getCircleId());
            if (circle != null) {
                vo.setCircleName(circle.getName());
            }
        }
        if (p.getUserId() != null) {
            User u = userMapper.selectById(p.getUserId());
            if (u != null) {
                PostAuthorVO author = new PostAuthorVO();
                author.setId(u.getId());
                author.setNickname(StrUtil.isNotBlank(u.getNickname()) ? u.getNickname() : u.getUsername());
                author.setAvatar(u.getAvatar());
                vo.setAuthor(author);
            }
        }
        if (p.getRobotId() != null) {
            Robot r = robotMapper.selectById(p.getRobotId());
            if (r != null) {
                vo.setRobotName(r.getName());
                vo.setRobotCover(r.getCoverImage());
            }
        }
        if (p.getBrandId() != null) {
            Brand b = brandMapper.selectById(p.getBrandId());
            if (b != null) {
                vo.setBrandName(b.getName());
            }
        }
        if (currentUserId != null) {
            vo.setLiked(likeService.check(currentUserId, "post", p.getId()));
            vo.setFavorited(favoriteService.check(currentUserId, "post", p.getId()));
            vo.setCanDelete(currentUserId.equals(p.getUserId()));
        } else {
            vo.setLiked(false);
            vo.setFavorited(false);
            vo.setCanDelete(false);
        }
        return vo;
    }
}
