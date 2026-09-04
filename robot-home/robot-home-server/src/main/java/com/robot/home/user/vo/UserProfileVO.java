package com.robot.home.user.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户主页信息（对外脱敏）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserProfileVO extends UserVO {

    private String intro;
    private Integer fansCount;
    private Integer followCount;
    private Integer postCount;
}
