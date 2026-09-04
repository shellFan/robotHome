package com.robot.home.ranking.vo;

import lombok.Data;

/**
 * 榜单类型
 */
@Data
public class RankingTypeVO {

    private String code;
    private String name;

    public RankingTypeVO() {
    }

    public RankingTypeVO(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
