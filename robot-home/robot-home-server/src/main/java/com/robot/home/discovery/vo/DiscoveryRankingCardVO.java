package com.robot.home.discovery.vo;

import lombok.Data;

import java.util.List;

/**
 * 发现页榜单卡片
 */
@Data
public class DiscoveryRankingCardVO {

    /** 榜单类型: hot/humanoid/quadruped/service/industrial/family/dev/follow/favorite/discussion/review/new_product */
    private String rankType;
    /** 榜单名称 */
    private String rankName;
    /** 榜单描述 */
    private String description;
    /** 榜单机器人（前N名） */
    private List<DiscoveryRankItemVO> items;
}