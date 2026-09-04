package com.robot.home.search.vo;

import lombok.Data;

/**
 * 热搜词
 */
@Data
public class HotSearchVO {

    private String keyword;
    private Integer searchCount;
    private Integer rank;
}
