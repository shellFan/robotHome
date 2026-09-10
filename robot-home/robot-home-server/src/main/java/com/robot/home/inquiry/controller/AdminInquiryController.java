package com.robot.home.inquiry.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.inquiry.dto.InquiryFollowDTO;
import com.robot.home.inquiry.entity.Inquiry;
import com.robot.home.inquiry.entity.InquiryFollow;
import com.robot.home.inquiry.mapper.InquiryMapper;
import com.robot.home.inquiry.service.InquiryFollowService;
import com.robot.home.security.RequirePermission;
import com.robot.home.security.UserContext;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台询价管理：查看 / 改状态 / 写跟进记录
 */
@RestController
@RequestMapping("/api/admin/inquiries")
public class AdminInquiryController {

    @Resource
    private InquiryMapper inquiryMapper;
    @Resource
    private InquiryFollowService inquiryFollowService;

    @GetMapping
    @RequirePermission("inquiry:list")
    public Result<PageResult<Inquiry>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Inquiry> page = new Page<>(pn, ps);
        IPage<Inquiry> result = inquiryMapper.selectPage(page, Wrappers.<Inquiry>lambdaQuery()
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .likeRight(Inquiry::getName, keyword)
                        .or().like(Inquiry::getPhone, keyword)
                        .or().likeRight(Inquiry::getRobotName, keyword)
                        .or().likeRight(Inquiry::getCompanyName, keyword))
                .eq(status != null, Inquiry::getStatus, status)
                .orderByDesc(Inquiry::getCreateTime));
        return Result.success(PageResult.of(pn, ps, result.getTotal(), result.getRecords()));
    }

    @GetMapping("/{id}")
    @RequirePermission("inquiry:list")
    public Result<Inquiry> detail(@PathVariable Long id) {
        Inquiry inquiry = inquiryMapper.selectById(id);
        if (inquiry == null) {
            throw new BusinessException("询价记录不存在");
        }
        return Result.success(inquiry);
    }

    /**
     * 更新状态
     */
    @PostMapping("/{id}/status")
    @RequirePermission("inquiry:handle")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam Integer status,
                                     @RequestParam(required = false) String handleNote) {
        Inquiry inquiry = inquiryMapper.selectById(id);
        if (inquiry == null) {
            throw new BusinessException("询价记录不存在");
        }
        Inquiry update = new Inquiry();
        update.setId(id);
        update.setStatus(status);
        if (StrUtil.isNotBlank(handleNote)) {
            update.setHandleNote(handleNote);
        }
        inquiryMapper.updateById(update);
        return Result.success();
    }

    /**
     * 追加跟进记录（handle_records 为 JSON 数组）
     */
    @PostMapping("/{id}/records")
    @RequirePermission("inquiry:handle")
    public Result<Void> addRecord(@PathVariable Long id,
                                  @RequestParam String content,
                                  @RequestParam(required = false) String operator) {
        Inquiry inquiry = inquiryMapper.selectById(id);
        if (inquiry == null) {
            throw new BusinessException("询价记录不存在");
        }
        List<Object> records = new ArrayList<>();
        if (StrUtil.isNotBlank(inquiry.getHandleRecords())) {
            try {
                JSONArray array = JSONUtil.parseArray(inquiry.getHandleRecords());
                records.addAll(array);
            } catch (Exception ignored) {
                // 历史数据格式异常时重新开始记录
            }
        }
        Map<String, Object> record = new HashMap<>(4);
        record.put("time", LocalDateTime.now().toString());
        record.put("operator", StrUtil.isBlank(operator) ? "系统" : operator);
        record.put("content", content);
        records.add(record);

        inquiryMapper.update(null, new LambdaUpdateWrapper<Inquiry>()
                .eq(Inquiry::getId, id)
                .set(Inquiry::getHandleRecords, JSONUtil.toJsonStr(records))
                .set(Inquiry::getUpdateTime, LocalDateTime.now()));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("inquiry:handle")
    public Result<Void> delete(@PathVariable Long id) {
        inquiryMapper.deleteById(id);
        return Result.success();
    }

    /**
     * 各状态数量（供后台筛选标签展示）
     */
    @GetMapping("/status-count")
    @RequirePermission("inquiry:list")
    public Result<Map<String, Long>> statusCount() {
        Map<String, Long> map = new HashMap<>();
        String[] names = {"1", "2", "3", "4", "5"};
        for (String status : names) {
            map.put(status, inquiryMapper.selectCount(Wrappers.<Inquiry>lambdaQuery()
                    .eq(Inquiry::getStatus, Integer.valueOf(status))));
        }
        return Result.success(map);
    }

    /**
     * 添加跟进记录（Phase6 结构化跟进）
     * POST /api/admin/inquiries/{id}/follow
     */
    @PostMapping("/{id}/follow")
    @RequirePermission("inquiry:handle")
    public Result<InquiryFollow> addFollow(@PathVariable Long id,
                                            @RequestBody @Valid InquiryFollowDTO dto) {
        dto.setInquiryId(id);
        Long adminUserId = UserContext.getUserId();
        InquiryFollow follow = inquiryFollowService.addFollow(adminUserId, dto);
        return Result.success(follow);
    }

    /**
     * 查询跟进记录列表
     * GET /api/admin/inquiries/{id}/follows
     */
    @GetMapping("/{id}/follows")
    @RequirePermission("inquiry:list")
    public Result<List<InquiryFollow>> listFollows(@PathVariable Long id) {
        return Result.success(inquiryFollowService.listByInquiryId(id));
    }
}
