package com.robot.home.trust.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 机器人数据来源（信任体系）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("robot_data_source")
public class RobotDataSource extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long robotId;
    /** 来源类型: OFFICIAL/CRAWLER/MANUAL/COMMUNITY */
    private String sourceType;
    private String sourceName;
    private String sourceUrl;
    /** 信任权重 0-100 */
    private Integer trustWeight;
    /** 是否已验证 */
    private Integer verified;
    private LocalDateTime verifiedTime;
}