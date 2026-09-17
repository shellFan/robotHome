package com.robot.home.ranking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 排行榜快照（Redis ZSET 的 MySQL 持久化）
 * 对应表: ranking_snapshot
 * <p>
 * 定时任务每小时将 Redis ZSET 排行榜持久化到此表，
 * 用于排行榜历史追踪和 Redis 故障恢复
 */
@Data
@TableName("ranking_snapshot")
public class RankingSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 排行榜类型: hot/humanoid/quadruped/service/industrial/family/dev */
    private String rankType;

    /** 快照日期 */
    private Date snapshotDate;

    /** 机器人ID */
    private Long robotId;

    /** 热度分 */
    private Long hotScore;

    /** 排名 */
    private Integer rankNo;

    /** 创建时间 */
    private Date createTime;
}