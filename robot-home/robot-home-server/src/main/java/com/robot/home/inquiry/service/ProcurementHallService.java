package com.robot.home.inquiry.service;

import com.robot.home.common.PageResult;
import com.robot.home.inquiry.vo.InquiryVO;

/**
 * 采购需求大厅服务
 */
public interface ProcurementHallService {

    /** 开放需求列表(脱敏) */
    PageResult<InquiryVO> hallList(String category, String usageScene, Integer pageNum, Integer pageSize);

    /** 需求详情(脱敏) */
    InquiryVO hallDetail(Long id);

    /** 管理端: 采购需求列表(含完整信息) */
    PageResult<InquiryVO> adminList(Integer status, String requirementType, Integer pageNum, Integer pageSize);

    /** 管理端: 分配需求 */
    void assign(Long inquiryId, Long adminUserId);

    /** 管理端: 更新线索评分 */
    void updateLeadScore(Long inquiryId, Integer score);
}