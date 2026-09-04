package com.robot.home.collector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.robot.home.collector.entity.CrawlerMatchRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 匹配记录 Mapper
 */
@Mapper
public interface CrawlerMatchRecordMapper extends BaseMapper<CrawlerMatchRecord> {
}