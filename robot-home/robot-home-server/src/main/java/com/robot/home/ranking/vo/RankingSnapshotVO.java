package com.robot.home.ranking.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Phase9: 排行榜快照项（带排名变化和上榜原因）
 */
@Data
public class RankingSnapshotVO {

    private Long id;
    /** 排行榜类型 */
    private String rankType;
    /** 快照日期 */
    private java.time.LocalDateTime snapshotDate;
    /** 机器人ID */
    private Long robotId;
    /** 机器人名称 */
    private String robotName;
    /** 机器人封面 */
    private String robotCoverImage;
    /** 机器人副标题 */
    private String robotSubtitle;
    /** 品牌ID */
    private Long brandId;
    /** 品牌名称 */
    private String brandName;
    /** 指导价 */
    private BigDecimal guidePrice;
    /** 热度分 */
    private Long hotScore;
    /** 当前排名 */
    private Integer rankNo;
    /** 上期排名 */
    private Integer prevRankNo;
    /** 排名变化（正数上升，负数下降，0不变，null新上榜） */
    private Integer rankChange;
    /** 上榜原因代码 */
    private String reasonCode;
    /** 上榜原因描述 */
    private String reasonText;
    /** 评分 */
    private BigDecimal score;
    /** 收藏数 */
    private Integer favoriteCount;
    /** 讨论数 */
    private Integer discussionCount;
    /** 关注数 */
    private Integer followCount;
}