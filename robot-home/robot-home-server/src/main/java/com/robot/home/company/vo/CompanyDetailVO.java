package com.robot.home.company.vo;

import com.robot.home.brand.entity.Brand;
import com.robot.home.company.entity.Company;
import com.robot.home.robot.vo.RobotSummaryVO;
import lombok.Data;

import java.util.List;

/**
 * 企业详情视图对象
 */
@Data
public class CompanyDetailVO {

    private Company company;
    private List<Brand> brandList;
    private List<RobotSummaryVO> productList;
}
