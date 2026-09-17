package com.robot.home.correction.service;

import com.robot.home.common.PageResult;
import com.robot.home.correction.dto.ParamCorrectionDTO;
import com.robot.home.correction.vo.ParamCorrectionVO;

/**
 * 参数纠错服务
 */
public interface RobotParamCorrectionService {

    /**
     * 提交纠错
     */
    Long submit(Long userId, ParamCorrectionDTO dto);

    /**
     * 我的纠错列表
     */
    PageResult<ParamCorrectionVO> myCorrections(Long userId, Integer pageNum, Integer pageSize);

    // ---- 管理端 ----

    /**
     * 管理端分页（可按状态/robotId筛选）
     */
    PageResult<ParamCorrectionVO> adminPage(Integer status, Long robotId, Integer pageNum, Integer pageSize);

    /**
     * 审核纠错（采纳/拒绝）
     * 采纳时自动更新RobotParamValue
     */
    void audit(Long reviewerId, Long correctionId, Integer status, String reviewNote);
}