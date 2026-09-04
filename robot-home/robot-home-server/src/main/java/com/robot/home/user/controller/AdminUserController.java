package com.robot.home.user.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.security.RequirePermission;
import com.robot.home.user.entity.User;
import com.robot.home.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 后台前台用户管理
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @Resource
    private UserService userService;

    @GetMapping
    @RequirePermission("user:list")
    public Result<PageResult<User>> page(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "20") Integer pageSize) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("keyword", keyword);
        params.put("status", status);
        params.put("pageNum", pageNum);
        params.put("pageSize", pageSize);
        com.baomidou.mybatisplus.core.metadata.IPage<User> page = userService.pageList(params);
        // 不返回密码
        for (User u : page.getRecords()) {
            u.setPassword(null);
        }
        return Result.success(PageResult.of((int) page.getCurrent(), (int) page.getSize(),
                page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    @RequirePermission("user:list")
    public Result<User> detail(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    /**
     * 封禁 / 解封
     */
    @PostMapping("/{id}/status")
    @RequirePermission("user:edit")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        userService.updateById(user);
        return Result.success();
    }
}
