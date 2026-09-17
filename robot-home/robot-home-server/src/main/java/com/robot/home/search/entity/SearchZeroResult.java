package com.robot.home.search.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 零结果搜索记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("search_zero_result")
public class SearchZeroResult extends BaseEntity {

    private String normalizedKeyword;
    private Integer searchCount;
    private LocalDateTime lastSearchTime;
    /** ADD_ALIAS/ADD_ROBOT/ADD_BRAND/IGNORE */
    private String suggestedAction;
}