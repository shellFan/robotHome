package com.robot.home.search.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 搜索历史
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("search_history")
public class SearchHistory extends IdEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String keyword;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
