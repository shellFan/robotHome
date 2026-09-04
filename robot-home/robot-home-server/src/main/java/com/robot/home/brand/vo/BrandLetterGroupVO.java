package com.robot.home.brand.vo;

import lombok.Data;

import java.util.List;

/**
 * 品牌首字母分组
 */
@Data
public class BrandLetterGroupVO {

    private String letter;
    private List<BrandListVO> brands;
}
