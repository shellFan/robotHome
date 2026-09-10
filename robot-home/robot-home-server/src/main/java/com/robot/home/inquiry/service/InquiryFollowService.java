package com.robot.home.inquiry.service;

import com.robot.home.inquiry.dto.InquiryFollowDTO;
import com.robot.home.inquiry.entity.InquiryFollow;

import java.util.List;

/**
 * 询价跟进服务
 */
public interface InquiryFollowService {

    /**
     * 添加跟进记录（同时更新询价状态）
     *
     * @param adminUserId 管理员ID
     * @param dto         跟进DTO
     * @return 跟进记录
     */
    InquiryFollow addFollow(Long adminUserId, InquiryFollowDTO dto);

    /**
     * 查询询价的跟进记录列表
     *
     * @param inquiryId 询价ID
     * @return 跟进记录列表（按时间倒序）
     */
    List<InquiryFollow> listByInquiryId(Long inquiryId);

    /**
     * 合法的跟进类型
     */
    List<String> VALID_FOLLOW_TYPES = java.util.Arrays.asList("CONTACTED", "FOLLOWING", "CLOSED", "INVALID", "NOTE");
}