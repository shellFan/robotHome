package com.robot.home.selection.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 机器人选型搜索参数
 */
@Data
public class SelectionSearchDTO {

    private Long userId;
    private String category;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String usage;
    private List<Long> brandIds;
    /** 动态参数过滤: key=参数名, value=期望值 */
    private Map<String, String> filters;
    private String sort;
    private Integer pageNum;
    private Integer pageSize;
}