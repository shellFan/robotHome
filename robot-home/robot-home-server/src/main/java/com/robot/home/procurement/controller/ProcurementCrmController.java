package com.robot.home.procurement.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.procurement.entity.ProcurementFollowRecord;
import com.robot.home.procurement.service.ProcurementCrmService;
import com.robot.home.procurement.vo.ProcurementCrmVO;
import com.robot.home.security.RequirePermission;
import com.robot.home.security.UserContext;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 采购CRM管理接口（后台）
 * <p>
 * 所有接口需要 procurement:crm 权限
 */
@RestController
@RequestMapping("/api/admin/procurements")
public class ProcurementCrmController {

    @Resource
    private ProcurementCrmService procurementCrmService;

    /**
     * CRM列表（支持筛选）
     * GET /api/admin/procurements
     */
    @GetMapping
    @RequirePermission("procurement:crm")
    public Result<PageResult<ProcurementCrmVO>> crmList(
            @RequestParam(required = false) String pipelineStatus,
            @RequestParam(required = false) Long crmOwner,
            @RequestParam(required = false) Integer crmPriority,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(procurementCrmService.crmList(pipelineStatus, crmOwner, crmPriority, keyword, pageNum, pageSize));
    }

    /**
     * CRM详情（含跟进记录+响应列表）
     * GET /api/admin/procurements/{id}
     */
    @GetMapping("/{id}")
    @RequirePermission("procurement:crm")
    public Result<ProcurementCrmVO> crmDetail(@PathVariable Long id) {
        return Result.success(procurementCrmService.crmDetail(id));
    }

    /**
     * 更新Pipeline状态
     * POST /api/admin/procurements/{id}/pipeline
     */
    @PostMapping("/{id}/pipeline")
    @RequirePermission("procurement:crm")
    public Result<Void> updatePipeline(@PathVariable Long id,
                                        @RequestParam @NotBlank(message = "Pipeline状态不能为空") String pipelineStatus) {
        Long operatorId = SecurityUtils.requireUserId();
        String operatorName = getOperatorName();
        procurementCrmService.updatePipelineStatus(id, pipelineStatus, operatorId, operatorName);
        return Result.success();
    }

    /**
     * 分配CRM负责人
     * POST /api/admin/procurements/{id}/assign
     */
    @PostMapping("/{id}/assign")
    @RequirePermission("procurement:crm")
    public Result<Void> assignOwner(@PathVariable Long id,
                                     @RequestParam @NotNull(message = "负责人ID不能为空") Long crmOwner) {
        Long operatorId = SecurityUtils.requireUserId();
        String operatorName = getOperatorName();
        procurementCrmService.assignOwner(id, crmOwner, operatorId, operatorName);
        return Result.success();
    }

    /**
     * 添加跟进记录
     * POST /api/admin/procurements/{id}/follow
     */
    @PostMapping("/{id}/follow")
    @RequirePermission("procurement:crm")
    public Result<ProcurementFollowRecord> addFollow(@PathVariable Long id,
                                                      @RequestParam @NotBlank(message = "操作类型不能为空") String action,
                                                      @RequestParam @NotBlank(message = "跟进内容不能为空") String content,
                                                      @RequestParam(required = false) LocalDateTime nextFollowTime) {
        Long operatorId = SecurityUtils.requireUserId();
        String operatorName = getOperatorName();
        return Result.success(procurementCrmService.addFollowRecord(id, action, content, nextFollowTime, operatorId, operatorName));
    }

    /**
     * 跟进记录列表
     * GET /api/admin/procurements/{id}/follows
     */
    @GetMapping("/{id}/follows")
    @RequirePermission("procurement:crm")
    public Result<List<ProcurementFollowRecord>> listFollows(@PathVariable Long id) {
        return Result.success(procurementCrmService.listFollowRecords(id));
    }

    /**
     * Pipeline统计
     * GET /api/admin/procurements/pipeline-stats
     */
    @GetMapping("/pipeline-stats")
    @RequirePermission("procurement:crm")
    public Result<Map<String, Long>> pipelineStats() {
        return Result.success(procurementCrmService.pipelineStats());
    }

    /**
     * 获取当前操作人姓名
     */
    private String getOperatorName() {
        com.robot.home.security.LoginUser user = UserContext.getUser();
        return user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : "系统";
    }
}