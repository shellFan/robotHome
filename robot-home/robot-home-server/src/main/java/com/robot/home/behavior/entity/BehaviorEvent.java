package com.robot.home.behavior.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 行为事件实体
 * 记录用户行为用于热度计算、推荐、统计分析
 */
@Data
@TableName("behavior_event")
public class BehaviorEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID（匿名浏览可为空） */
    private Long userId;

    /** 会话ID */
    private String sessionId;

    /** 事件类型：VIEW/SEARCH/CLICK/FAVORITE/UNFAVORITE/COMPARE/INQUIRY/SHARE/COMMENT/FOLLOW */
    private String eventType;

    /** 业务类型：robot/article/video/post/tutorial/brand/company */
    private String bizType;

    /** 业务对象ID */
    private Long bizId;

    /** 扩展信息（搜索词/对比IDs等） */
    private String extra;

    /** IP地址 */
    private String ip;

    /** User-Agent */
    private String userAgent;

    private Date createTime;
}