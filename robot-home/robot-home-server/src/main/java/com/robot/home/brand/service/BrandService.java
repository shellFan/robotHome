package com.robot.home.brand.service;

import com.robot.home.brand.vo.BrandDetailVO;
import com.robot.home.brand.vo.BrandLetterGroupVO;
import com.robot.home.brand.vo.BrandListVO;
import com.robot.home.common.PageResult;
import com.robot.home.robot.vo.RobotSummaryVO;

import java.util.List;

/**
 * 品牌服务
 */
public interface BrandService {

    PageResult<BrandListVO> page(String keyword, String initial, Boolean hot, Integer pageNum, Integer pageSize);

    BrandDetailVO detail(Long id);

    PageResult<RobotSummaryVO> robots(Long brandId, Integer pageNum, Integer pageSize);

    /**
     * 按首字母分组（品牌库字母索引）
     */
    List<BrandLetterGroupVO> groupByLetter();

    List<BrandListVO> hot(int limit);
}
