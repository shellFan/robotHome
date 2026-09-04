package com.robot.home.robot.dto;

import lombok.Data;

import java.util.List;

/**
 * 参数值批量保存请求体
 */
@Data
public class ParamValueSaveDTO {

    private Long robotId;
    private List<Item> items;

    @Data
    public static class Item {
        private Long defId;
        private String value;
    }
}
