package com.robot.home.comment.service;

import com.robot.home.comment.dto.CommentDTO;
import com.robot.home.comment.vo.CommentVO;
import com.robot.home.common.PageResult;

/**
 * 通用评论服务：文章 / 视频 / 机器人 / 帖子 / 教程共用
 */
public interface CommentService {

    /**
     * 一级评论分页（每条附带前 3 条回复）
     */
    PageResult<CommentVO> list(String bizType, Long bizId, Long currentUserId, Integer pageNum, Integer pageSize);

    /**
     * 某条评论的回复列表
     */
    PageResult<CommentVO> replies(Long parentId, Long currentUserId, Integer pageNum, Integer pageSize);

    /**
     * 发表评论，返回新评论 id
     */
    Long add(Long userId, CommentDTO dto);

    /**
     * 删除评论（本人或管理员）
     */
    void delete(Long userId, Long commentId, boolean isAdmin);

    /**
     * 给评论点赞
     */
    boolean like(Long userId, Long commentId);

    /**
     * 我的评论
     */
    PageResult<CommentVO> myComments(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 业务对象评论总数
     */
    long count(String bizType, Long bizId);
}
