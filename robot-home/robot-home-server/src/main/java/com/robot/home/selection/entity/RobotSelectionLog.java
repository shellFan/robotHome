package com.robot.home.selection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("robot_selection_log")
public class RobotSelectionLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String category;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String usage;
    private Integer resultCount;
    private LocalDateTime createTime;
}