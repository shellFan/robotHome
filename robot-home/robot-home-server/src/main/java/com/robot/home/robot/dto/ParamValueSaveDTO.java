package com.robot.home.robot.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 参数值批量保存请求体
 */
@Data
public class ParamValueSaveDTO {

    @NotNull(message = "机器人不能为空")
    private Long robotId;

    @NotEmpty(message = "参数值列表不能为空")
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "参数定义不能为空")
        private Long defId;

        @Size(max = 255, message = "参数值过长")
        private String value;
    }
}