package com.robot.home.search.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 热搜词
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hot_search")
public class HotSearch extends IdEntity {

    private static final long serialVersionUID = 1L;

    private String keyword;
    private Integer searchCount;
    private Integer sort;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
