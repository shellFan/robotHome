package com.robot.home.company.service;

import com.robot.home.common.PageResult;
import com.robot.home.company.vo.CompanyDetailVO;
import com.robot.home.company.vo.CompanyListVO;
import com.robot.home.robot.vo.RobotSummaryVO;

import java.util.List;

/**
 * 企业服务
 */
public interface CompanyService {

    PageResult<CompanyListVO> page(String keyword, String region, Integer pageNum, Integer pageSize);

    CompanyDetailVO detail(Long id);

    PageResult<RobotSummaryVO> robots(Long companyId, Integer pageNum, Integer pageSize);

    /**
     * 地区列表（用于筛选）
     */
    List<String> regions();

    List<CompanyListVO> hot(int limit);
}
