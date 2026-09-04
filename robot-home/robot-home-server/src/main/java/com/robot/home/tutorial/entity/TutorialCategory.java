package com.robot.home.tutorial.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 教程分类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tutorial_category")
public class TutorialCategory extends BaseEntity {

    private String name;
    private Integer sort;
    private Integer status;
}
