package com.robot.home.inquiry.controller;

import com.robot.home.common.PageResult;
import com.robot.home.inquiry.service.ProcurementHallService;
import com.robot.home.inquiry.vo.InquiryVO;
import com.robot.home.ratelimit.annotation.RateLimit;
import com.robot.home.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 采购需求大厅API
 * 公开接口: /api/procurement/hall
 * 管理接口: /api/admin/procurement/** (由AdminInterceptor保护)
 */
@RestController
@RequiredArgsConstructor
public class ProcurementHallController {

    private final ProcurementHallService procurementHallService;

    /** 开放需求列表(脱敏) */
    @GetMapping("/api/procurement/hall")
    @RateLimit(action = "procurement_hall", windowSeconds = 60, maxRequests = 30, dimension = "IP")
    public PageResult<InquiryVO> hallList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String usageScene,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return procurementHallService.hallList(category, usageScene, pageNum, pageSize);
    }

    /** 需求详情(脱敏) */
    @GetMapping("/api/procurement/hall/{id}")
    public InquiryVO hallDetail(@PathVariable Long id) {
        return procurementHallService.hallDetail(id);
    }

    /** 管理端: 采购需求列表 */
    @GetMapping("/api/admin/procurement/inquiries")
    @RequirePermission("procurement:crm")
    public PageResult<InquiryVO> adminList(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String requirementType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return procurementHallService.adminList(status, requirementType, pageNum, pageSize);
    }

    /** 管理端: 分配需求 */
    @PutMapping("/api/admin/procurement/inquiries/{id}/assign")
    @RequirePermission("procurement:crm")
    public void assign(@PathVariable Long id, @RequestParam Long adminUserId) {
        procurementHallService.assign(id, adminUserId);
    }

    /** 管理端: 更新线索评分 */
    @PutMapping("/api/admin/procurement/inquiries/{id}/lead-score")
    @RequirePermission("procurement:crm")
    public void updateLeadScore(@PathVariable Long id, @RequestParam Integer score) {
        procurementHallService.updateLeadScore(id, score);
    }
}