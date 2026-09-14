package com.robot.home.common.base;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;

/**
 * 仅含主键的轻量基类：适用于无逻辑删除字段的关联表 / 日志表
 */
public abstract class IdEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // --- getter/setter ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}