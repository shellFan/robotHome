package com.robot.home.search.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 搜索聚合结果：按业务类型分组
 */
@Data
public class SearchResultVO {

    private String keyword;
    private List<SearchItemVO> robots;
    private List<SearchItemVO> brands;
    private List<SearchItemVO> companies;
    private List<SearchItemVO> articles;
    private List<SearchItemVO> videos;
    private List<SearchItemVO> tutorials;
    private List<SearchItemVO> posts;
    /** 各类型命中数量 */
    private Map<String, Long> counts;
}
