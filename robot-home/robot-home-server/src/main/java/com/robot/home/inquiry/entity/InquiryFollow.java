package com.robot.home.inquiry.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 询价跟进记录
 * 对应表: inquiry_follow
 * <p>
 * 状态流转: CONTACTED → FOLLOWING → CLOSED/INVALID
 * NOTE 类型为纯备注，不改变状态
 */
@Data
@TableName("inquiry_follow")
public class InquiryFollow {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 询价ID */
    private Long inquiryId;

    /** 跟进人ID（管理员） */
    private Long followUserId;

    /** 跟进类型: CONTACTED/FOLLOWING/CLOSED/INVALID/NOTE */
    private String followType;

    /** 跟进内容 */
    private String content;

    /** 跟进后询价状态 */
    private Integer afterStatus;

    /** 创建时间 */
    private Date createTime;
}