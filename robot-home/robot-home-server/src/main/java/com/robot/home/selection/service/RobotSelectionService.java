package com.robot.home.selection.service;

import com.robot.home.common.PageResult;
import com.robot.home.selection.dto.SelectionSearchDTO;
import com.robot.home.selection.vo.SelectionResultVO;

import java.util.List;
import java.util.Map;

/**
 * 机器人选型服务
 */
public interface RobotSelectionService {

    /** 选型搜索 */
    PageResult<SelectionResultVO> search(SelectionSearchDTO dto);

    /** 获取可用筛选条件(按分类) */
    Map<String, Object> getFilters(String category);

    /** 记录选型日志 */
    void logSelection(SelectionSearchDTO dto, Integer resultCount);
}