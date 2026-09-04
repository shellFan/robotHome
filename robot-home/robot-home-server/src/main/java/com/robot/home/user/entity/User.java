package com.robot.home.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 前台用户（PC / 小程序统一用户体系）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    /** 用户名（唯一） */
    private String username;
    /** 密码（哈希存储） */
    private String password;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 昵称 */
    private String nickname;
    /** 个人简介 */
    private String intro;
    /** 头像 */
    private String avatar;
    /** 性别：0未知 1男 2女 */
    private Integer gender;
    /** 生日 */
    private LocalDate birthday;
    /** 省份 */
    private String province;
    /** 城市 */
    private String city;
    /** 粉丝数 */
    private Integer fansCount;
    /** 关注数 */
    private Integer followCount;
    /** 发帖数 */
    private Integer postCount;
    /** 微信 openid */
    private String openid;
    /** 状态：0禁用 1正常 */
    private Integer status;
    /** 用户类型：1普通用户 2后台管理员（冗余，便于 JWT） */
    private Integer userType;
    /** 注册来源：pc / miniapp */
    private String source;
    /** 最后登录时间 */
    private java.time.LocalDateTime lastLoginTime;
    /** 最后登录 IP */
    private String lastLoginIp;
}
