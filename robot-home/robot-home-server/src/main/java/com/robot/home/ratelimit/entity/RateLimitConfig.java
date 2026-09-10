package com.robot.home.ratelimit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 限流配置
 * 对应表: rate_limit_config
 * <p>
 * 支持10个接口的限流配置: login/sms/search/inquiry/comment/post/like/follow/feedback/upload
 */
@Data
@TableName("rate_limit_config")
public class RateLimitConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 限流接口标识: login/sms/search/inquiry/comment/post/like/follow/feedback/upload */
    private String action;

    /** 接口描述 */
    private String description;

    /** 时间窗口（秒） */
    private Integer windowSeconds;

    /** 窗口内最大请求数 */
    private Integer maxRequests;

    /** 限流维度: IP / USER / IP_USER */
    private String dimension;

    /** 是否启用: 1=启用, 0=禁用 */
    private Integer enabled;
}