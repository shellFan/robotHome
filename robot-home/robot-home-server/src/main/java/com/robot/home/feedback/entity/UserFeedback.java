package com.robot.home.feedback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户反馈
 * 对应表: user_feedback
 */
@Data
@TableName("user_feedback")
public class UserFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID（可空，支持游客反馈） */
    private Long userId;

    /** 反馈类型: bug/feature/improvement/other */
    private String feedbackType;

    /** 反馈内容 */
    private String content;

    /** 联系方式（可选） */
    private String contact;

    /** 页面URL */
    private String pageUrl;

    /** 处理状态: 0=待处理, 1=已处理 */
    private Integer status;

    /** 处理备注 */
    private String handleNote;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}