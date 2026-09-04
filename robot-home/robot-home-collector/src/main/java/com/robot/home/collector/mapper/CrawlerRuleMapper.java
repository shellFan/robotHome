package com.robot.home.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.collector.entity.CrawlerRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采集规则 Mapper
 */
@Mapper
public interface CrawlerRuleMapper extends BaseMapper<CrawlerRule> {
}