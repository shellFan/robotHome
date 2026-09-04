package com.robot.home.sys.vo;

import com.robot.home.sys.entity.SysMenu;
import lombok.Data;

import java.util.List;

/**
 * 后台登录用户信息（含菜单与权限）
 */
@Data
public class AdminInfoVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private List<String> roles;
    private List<String> permissions;
    private List<SysMenu> menus;
}
