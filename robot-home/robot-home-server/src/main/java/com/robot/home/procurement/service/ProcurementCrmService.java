package com.robot.home.procurement.service;

import com.robot.home.common.PageResult;
import com.robot.home.procurement.entity.ProcurementFollowRecord;
import com.robot.home.procurement.vo.ProcurementCrmVO;

import java.util.List;
import java.util.Map;

/**
 * 采购CRM服务
 * <p>
 * 管理采购线索的Pipeline流转、负责人分配、跟进记录等CRM功能
 */
public interface ProcurementCrmService {

    /**
     * CRM列表（支持筛选）
     *
     * @param pipelineStatus Pipeline状态筛选
     * @param crmOwner       负责人ID筛选
     * @param crmPriority    CRM优先级筛选
     * @param keyword        关键词搜索（公司名/联系人/机器人名）
     * @param pageNum        页码
     * @param pageSize       每页条数
     * @return CRM分页列表
     */
    PageResult<ProcurementCrmVO> crmList(String pipelineStatus, Long crmOwner, Integer crmPriority,
                                          String keyword, Integer pageNum, Integer pageSize);

    /**
     * CRM详情（含跟进记录+响应列表）
     *
     * @param procurementId 采购需求ID
     * @return CRM详情
     */
    ProcurementCrmVO crmDetail(Long procurementId);

    /**
     * 更新Pipeline状态
     *
     * @param procurementId   采购需求ID
     * @param newPipelineStatus 新的Pipeline状态
     * @param operatorId      操作人ID
     * @param operatorName    操作人姓名
     */
    void updatePipelineStatus(Long procurementId, String newPipelineStatus, Long operatorId, String operatorName);

    /**
     * 分配CRM负责人
     *
     * @param procurementId 采购需求ID
     * @param crmOwner      负责人ID
     * @param operatorId    操作人ID
     * @param operatorName  操作人姓名
     */
    void assignOwner(Long procurementId, Long crmOwner, Long operatorId, String operatorName);

    /**
     * 添加跟进记录
     *
     * @param procurementId 采购需求ID
     * @param action        操作类型
     * @param content       跟进内容
     * @param nextFollowTime 下次跟进时间
     * @param operatorId    操作人ID
     * @param operatorName  操作人姓名
     * @return 跟进记录
     */
    ProcurementFollowRecord addFollowRecord(Long procurementId, String action, String content,
                                             java.time.LocalDateTime nextFollowTime,
                                             Long operatorId, String operatorName);

    /**
     * 跟进记录列表
     *
     * @param procurementId 采购需求ID
     * @return 跟进记录列表
     */
    List<ProcurementFollowRecord> listFollowRecords(Long procurementId);

    /**
     * Pipeline统计：各状态数量
     *
     * @return 状态 -> 数量
     */
    Map<String, Long> pipelineStats();
}