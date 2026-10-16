package com.robot.home.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.procurement.entity.ProcurementResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 采购需求响应 Mapper
 */
@Mapper
public interface ProcurementResponseMapper extends BaseMapper<ProcurementResponse> {

    /**
     * 查询指定采购需求+企业的任意响应记录（绕过@TableLogic，含逻辑删除和已撤回）。
     * 用于DuplicateKeyException恢复场景：唯一索引uk_procurement_company不含deleted列，
     * 逻辑删除或WITHDRAWN记录仍占位时，需此查询定位冲突记录。
     */
    @Select("SELECT * FROM procurement_response WHERE procurement_id = #{procurementId} AND company_id = #{companyId} ORDER BY id DESC LIMIT 1")
    ProcurementResponse selectAnyByProcurementAndCompany(@Param("procurementId") Long procurementId,
                                                          @Param("companyId") Long companyId);

    /**
     * 恢复逻辑删除的响应记录并更新内容（绕过@TableLogic，@TableLogic的update会自动追加deleted=0导致无法恢复）。
     */
    @Update("UPDATE procurement_response SET deleted = 0, status = 'SUBMITTED', " +
            "solution = #{solution}, price_description = #{priceDescription}, " +
            "delivery_description = #{deliveryDescription}, contact_description = #{contactDescription}, " +
            "robot_ids = #{robotIds}, contact_user_id = #{contactUserId}, event_key = #{eventKey}, " +
            "update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreDeletedResponse(@Param("id") Long id,
                                @Param("solution") String solution,
                                @Param("priceDescription") String priceDescription,
                                @Param("deliveryDescription") String deliveryDescription,
                                @Param("contactDescription") String contactDescription,
                                @Param("robotIds") String robotIds,
                                @Param("contactUserId") Long contactUserId,
                                @Param("eventKey") String eventKey);
}