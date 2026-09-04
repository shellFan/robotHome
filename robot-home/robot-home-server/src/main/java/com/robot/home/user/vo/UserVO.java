package com.robot.home.user.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户对外视图（不含密码）
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private Integer gender;
    private String province;
    private String city;
    private Integer status;
    private Integer userType;
    private String source;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
}
