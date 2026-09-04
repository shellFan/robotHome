package com.robot.home.security;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 登录用户信息（存入 JWT / ThreadLocal）
 */
@Data
@Accessors(chain = true)
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private Integer userType;
    private String token;
}
