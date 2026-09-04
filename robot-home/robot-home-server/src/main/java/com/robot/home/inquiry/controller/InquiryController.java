package com.robot.home.inquiry.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.inquiry.dto.InquiryDTO;
import com.robot.home.inquiry.service.InquiryService;
import com.robot.home.inquiry.vo.InquiryVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

/**
 * 询价
 */
@RestController
@RequestMapping("/api/inquiries")
public class InquiryController {

    @Resource
    private InquiryService inquiryService;

    /**
     * 提交询价（支持未登录，userId 为空时记录为游客询价）
     */
    @PostMapping
    public Result<Map<String, Object>> submit(@RequestBody @Valid InquiryDTO dto) {
        Long id = inquiryService.submit(SecurityUtils.currentUserId(), dto);
        return Result.success(Collections.singletonMap("id", id));
    }

    @GetMapping("/my")
    public Result<PageResult<InquiryVO>> my(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(inquiryService.my(userId, pageNum, pageSize));
    }

    @GetMapping("/my/{id}")
    public Result<InquiryVO> myDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(inquiryService.myDetail(userId, id));
    }
}
