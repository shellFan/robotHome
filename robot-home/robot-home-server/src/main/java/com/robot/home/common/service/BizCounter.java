package com.robot.home.common.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.robot.home.article.mapper.ArticleMapper;
import com.robot.home.article.entity.Article;
import com.robot.home.comment.entity.Comment;
import com.robot.home.comment.mapper.CommentMapper;
import com.robot.home.community.entity.CommunityPost;
import com.robot.home.community.mapper.CommunityPostMapper;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.tutorial.entity.Tutorial;
import com.robot.home.tutorial.mapper.TutorialMapper;
import com.robot.home.video.entity.Video;
import com.robot.home.video.mapper.VideoMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 通用业务计数器：统一维护各业务表的浏览/点赞/收藏/评论/对比/询价计数
 * 避免每个业务各写一套计数逻辑
 */
@Component
public class BizCounter {

    public enum Field {
        VIEW("view_count"),
        LIKE("like_count"),
        FAVORITE("favorite_count"),
        COMMENT("comment_count"),
        COMPARE("compare_count"),
        INQUIRY("inquiry_count");

        private final String column;

        Field(String column) {
            this.column = column;
        }

        public String getColumn() {
            return column;
        }
    }

    @Resource
    private RobotMapper robotMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private VideoMapper videoMapper;
    @Resource
    private TutorialMapper tutorialMapper;
    @Resource
    private CommunityPostMapper postMapper;
    @Resource
    private CommentMapper commentMapper;

    /**
     * 增减业务计数
     *
     * @param bizType robot / article / video / tutorial / post / comment
     * @param bizId   业务 id
     * @param field   计数字段
     * @param delta   增量（可为负）
     */
    public void incr(String bizType, Long bizId, Field field, int delta) {
        if (bizId == null || delta == 0) {
            return;
        }
        String sql = field.getColumn() + " = " + field.getColumn() + " + (" + delta + ")";
        switch (bizType) {
            case "robot":
                robotMapper.update(null, new LambdaUpdateWrapper<Robot>().eq(Robot::getId, bizId).setSql(sql));
                break;
            case "article":
                articleMapper.update(null, new LambdaUpdateWrapper<Article>().eq(Article::getId, bizId).setSql(sql));
                break;
            case "video":
                videoMapper.update(null, new LambdaUpdateWrapper<Video>().eq(Video::getId, bizId).setSql(sql));
                break;
            case "tutorial":
                tutorialMapper.update(null, new LambdaUpdateWrapper<Tutorial>().eq(Tutorial::getId, bizId).setSql(sql));
                break;
            case "post":
                postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>().eq(CommunityPost::getId, bizId).setSql(sql));
                break;
            case "comment":
                commentMapper.update(null, new LambdaUpdateWrapper<Comment>().eq(Comment::getId, bizId).setSql(sql));
                break;
            default:
                throw new IllegalArgumentException("不支持的业务类型: " + bizType);
        }
    }

    public void incr(String bizType, Long bizId, Field field) {
        incr(bizType, bizId, field, 1);
    }

    public void decr(String bizType, Long bizId, Field field) {
        incr(bizType, bizId, field, -1);
    }
}
