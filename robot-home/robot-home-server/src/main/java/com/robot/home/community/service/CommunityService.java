package com.robot.home.community.service;

import com.robot.home.common.PageResult;
import com.robot.home.community.dto.PostDTO;
import com.robot.home.community.vo.CircleVO;
import com.robot.home.community.vo.PostVO;

import java.util.List;

/**
 * 社区服务：圈子 / 帖子
 */
public interface CommunityService {

    List<CircleVO> circles(Long currentUserId);

    /**
     * 帖子分页：sort = latest / hot
     */
    PageResult<PostVO> posts(Long circleId, String topic, String keyword, String sort,
                             Long currentUserId, Integer pageNum, Integer pageSize);

    PostVO detail(Long id, Long currentUserId);

    /**
     * 发帖，返回帖子 id
     */
    Long create(Long userId, PostDTO dto);

    /**
     * 删除自己的帖子
     */
    void delete(Long userId, Long postId, boolean isAdmin);

    PageResult<PostVO> myPosts(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 热门话题
     */
    List<String> hotTopics(int limit);

    /**
     * 某机器人/品牌下的帖子
     */
    PageResult<PostVO> postsByTarget(String targetType, Long targetId, Integer pageNum, Integer pageSize);
}
