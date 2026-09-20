package com.robot.home.collection.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.IdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 收藏集项
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_collection_item")
public class UserCollectionItem extends IdEntity {

    private static final long serialVersionUID = 1L;

    private Long collectionId;
    private Long robotId;
    private String note;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}