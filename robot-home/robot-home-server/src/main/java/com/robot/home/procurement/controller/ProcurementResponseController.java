package com.robot.home.procurement.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.company.service.CompanyMemberService;
import com.robot.home.procurement.dto.ProcurementResponseDTO;
import com.robot.home.procurement.service.ProcurementResponseService;
import com.robot.home.procurement.vo.ProcurementResponseVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

/**
 * 采购需求响应接口（用户端/企业端）
 * <p>
 * 企业提交响应、查看响应列表、更新状态、撤回
 * <p>
 * 安全：所有涉及companyId的接口均通过CompanyMemberService校验企业成员身份，
 * 防止用户冒充其他企业提交响应
 */
@RestController
@RequestMapping("/api/procurement-responses")
public class ProcurementResponseController {

    @Resource
    private ProcurementResponseService procurementResponseService;
    @Resource
    private CompanyMemberService companyMemberService;

    /**
     * 提交响应
     * POST /api/procurement-responses
     * <p>
     * 安全修复：companyId仍由前端传入（支持多企业用户选择），
     * 但通过CompanyMemberService校验当前用户是否属于该企业
     */
    @PostMapping
    public Result<ProcurementResponseVO> submitResponse(
            @RequestBody @Valid ProcurementResponseDTO dto,
            @RequestParam Long companyId) {
        Long userId = SecurityUtils.requireUserId();
        // 安全校验：验证当前用户是否属于该企业
        companyMemberService.requireActiveMember(userId, companyId);
        return Result.success(procurementResponseService.submitResponse(userId, companyId, dto));
    }

    /**
     * 查看采购需求的响应列表（脱敏）
     * GET /api/procurement-responses/procurement/{procurementId}
     */
    @GetMapping("/procurement/{procurementId}")
    public Result<PageResult<ProcurementResponseVO>> listByProcurement(
            @PathVariable Long procurementId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long currentUserId = SecurityUtils.currentUserId();
        return Result.success(procurementResponseService.listByProcurement(procurementId, currentUserId, pageNum, pageSize));
    }

    /**
     * 我的响应列表
     * GET /api/procurement-responses/my
     * <p>
     * 安全修复：companyId必须经过企业成员校验；
     * 若不传companyId，则返回用户所有所属企业的响应
     */
    @GetMapping("/my")
    public Result<PageResult<ProcurementResponseVO>> myResponses(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        if (companyId != null) {
            // 安全校验：验证当前用户是否属于该企业
            companyMemberService.requireActiveMember(userId, companyId);
        }
        return Result.success(procurementResponseService.listMyResponses(userId, companyId, pageNum, pageSize));
    }

    /**
     * 更新响应状态
     * PUT /api/procurement-responses/{id}/status
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                      @RequestParam @NotBlank(message = "状态不能为空") String status) {
        Long userId = SecurityUtils.requireUserId();
        procurementResponseService.updateStatus(id, status, userId);
        return Result.success();
    }

    /**
     * 撤回响应
     * POST /api/procurement-responses/{id}/withdraw
     */
    @PostMapping("/{id}/withdraw")
    public Result<Void> withdraw(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        procurementResponseService.withdrawResponse(id, userId);
        return Result.success();
    }
}