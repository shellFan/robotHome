package com.robot.home.brand.vo;

import com.robot.home.brand.entity.Brand;
import com.robot.home.robot.vo.RobotSummaryVO;
import lombok.Data;

import java.util.List;

/**
 * 品牌详情视图对象
 */
@Data
public class BrandDetailVO {

    private Brand brand;
    private String companyName;
    private Long productCount;
    private List<RobotSummaryVO> products;
}
