package com.robot.home.procurement.service;

import com.robot.home.common.PageResult;
import com.robot.home.procurement.dto.ProcurementResponseDTO;
import com.robot.home.procurement.vo.ProcurementResponseVO;

import java.util.List;

/**
 * 采购需求响应服务
 * <p>
 * 企业提交响应、查看响应列表、更新状态、撤回等
 */
public interface ProcurementResponseService {

    /**
     * 企业提交响应
     * <p>
     * 防刷: uk_procurement_company唯一约束 + eventKey幂等
     *
     * @param userId    当前登录用户ID
     * @param companyId 企业ID
     * @param dto       响应DTO
     * @return 响应VO
     */
    ProcurementResponseVO submitResponse(Long userId, Long companyId, ProcurementResponseDTO dto);

    /**
     * 查看采购需求的响应列表（脱敏）
     *
     * @param procurementId 采购需求ID
     * @param currentUserId 当前登录用户ID（用于隐私判断）
     * @param pageNum       页码
     * @param pageSize      每页条数
     * @return 响应分页列表
     */
    PageResult<ProcurementResponseVO> listByProcurement(Long procurementId, Long currentUserId,
                                                         Integer pageNum, Integer pageSize);

    /**
     * 查看企业的响应列表
     *
     * @param companyId 企业ID
     * @param pageNum   页码
     * @param pageSize  每页条数
     * @return 响应分页列表
     */
    PageResult<ProcurementResponseVO> listByCompany(Long companyId, Integer pageNum, Integer pageSize);

    /**
     * 更新响应状态
     *
     * @param responseId 响应ID
     * @param newStatus  新状态
     * @param userId     当前登录用户ID（IDOR校验）
     */
    void updateStatus(Long responseId, String newStatus, Long userId);

    /**
     * 撤回响应
     *
     * @param responseId 响应ID
     * @param userId     当前登录用户ID（IDOR校验）
     */
    void withdrawResponse(Long responseId, Long userId);
}