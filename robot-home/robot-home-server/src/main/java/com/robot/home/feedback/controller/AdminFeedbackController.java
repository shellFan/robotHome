package com.robot.home.feedback.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.feedback.entity.UserFeedback;
import com.robot.home.feedback.mapper.UserFeedbackMapper;
import com.robot.home.security.RequirePermission;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 后台用户反馈管理：查看列表 / 处理 / 删除
 */
@RestController
@RequestMapping("/api/admin/feedbacks")
public class AdminFeedbackController {

    @Resource
    private UserFeedbackMapper userFeedbackMapper;

    /**
     * 分页查询反馈列表
     */
    @GetMapping
    @RequirePermission("feedback:list")
    public Result<PageResult<UserFeedback>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String feedbackType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<UserFeedback> page = new Page<>(pn, ps);
        IPage<UserFeedback> result = userFeedbackMapper.selectPage(page,
                Wrappers.<UserFeedback>lambdaQuery()
                        .and(StrUtil.isNotBlank(keyword), w -> w
                                .likeRight(UserFeedback::getContent, keyword)
                                .or().like(UserFeedback::getContact, keyword))
                        .eq(status != null, UserFeedback::getStatus, status)
                        .eq(StrUtil.isNotBlank(feedbackType), UserFeedback::getFeedbackType, feedbackType)
                        .orderByDesc(UserFeedback::getCreateTime));
        return Result.success(PageResult.of(pn, ps, result.getTotal(), result.getRecords()));
    }

    /**
     * 查看反馈详情
     */
    @GetMapping("/{id}")
    @RequirePermission("feedback:list")
    public Result<UserFeedback> detail(@PathVariable Long id) {
        UserFeedback feedback = userFeedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BusinessException("反馈记录不存在");
        }
        return Result.success(feedback);
    }

    /**
     * 处理反馈：标记已处理 + 处理备注
     */
    @PostMapping("/{id}/handle")
    @RequirePermission("feedback:handle")
    public Result<Void> handle(@PathVariable Long id,
                               @RequestParam(required = false) String handleNote) {
        UserFeedback feedback = userFeedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BusinessException("反馈记录不存在");
        }
        UserFeedback update = new UserFeedback();
        update.setId(id);
        update.setStatus(1); // 已处理
        update.setHandleNote(StrUtil.isBlank(handleNote) ? "已处理" : handleNote);
        update.setUpdateTime(new Date());
        userFeedbackMapper.updateById(update);
        return Result.success();
    }

    /**
     * 删除反馈
     */
    @DeleteMapping("/{id}")
    @RequirePermission("feedback:handle")
    public Result<Void> delete(@PathVariable Long id) {
        userFeedbackMapper.deleteById(id);
        return Result.success();
    }
}