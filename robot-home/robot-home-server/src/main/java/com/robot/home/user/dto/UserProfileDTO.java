package com.robot.home.user.dto;

import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 个人资料更新参数
 */
@Data
public class UserProfileDTO {

    @Size(max = 32, message = "昵称不能超过 32 字")
    private String nickname;

    @Size(max = 255, message = "头像地址过长")
    private String avatar;

    @Size(max = 255, message = "简介不能超过 255 字")
    private String intro;

    private Integer gender;

    @Size(max = 32, message = "省份过长")
    private String province;

    @Size(max = 32, message = "城市过长")
    private String city;

    @Pattern(regexp = "^$|^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    private String email;
}
