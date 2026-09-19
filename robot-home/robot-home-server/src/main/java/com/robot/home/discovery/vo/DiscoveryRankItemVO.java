package com.robot.home.discovery.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 发现页榜单项
 */
@Data
public class DiscoveryRankItemVO {

    private Long robotId;
    private String name;
    private String coverImage;
    private String subtitle;
    private Long brandId;
    private String brandName;
    private BigDecimal guidePrice;
    private Long hotScore;
    /** 排名 */
    private Integer rankNo;
    /** 排名变化: 正=上升, 负=下降, null=NEW */
    private Integer rankChange;
    /** 上榜原因码 */
    private String reasonCode;
    /** 上榜原因描述 */
    private String reasonText;
}