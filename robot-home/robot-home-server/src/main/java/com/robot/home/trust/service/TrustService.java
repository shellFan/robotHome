package com.robot.home.trust.service;

import com.robot.home.common.PageResult;
import com.robot.home.trust.vo.RobotChangeRecordVO;
import com.robot.home.trust.vo.RobotDataSourceVO;
import com.robot.home.trust.vo.RobotTrustVO;

import java.util.List;

/**
 * 信任体系服务接口
 */
public interface TrustService {

    /** 获取机器人可信度 */
    RobotTrustVO getTrust(Long robotId);

    /** 获取数据来源列表 */
    List<RobotDataSourceVO> getDataSources(Long robotId);

    /** 变更历史（分页） */
    PageResult<RobotChangeRecordVO> getChangeHistory(Long robotId, String changeType, Integer pageNum, Integer pageSize);

    /** 重算信任分（内部/admin） */
    void recalculateTrust(Long robotId);

    /** 添加数据来源 */
    void addDataSource(Long robotId, String sourceType, String sourceName, String sourceUrl);

    /** 记录变更 */
    void recordChange(Long robotId, String changeType, String fieldName, String fieldLabel,
                       String oldValue, String newValue, String sourceType);
}