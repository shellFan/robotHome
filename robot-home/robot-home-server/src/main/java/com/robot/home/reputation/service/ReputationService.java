package com.robot.home.reputation.service;

import com.robot.home.common.PageResult;
import com.robot.home.reputation.vo.ReputationEventVO;
import com.robot.home.reputation.vo.ReputationVO;

/**
 * P0-7: 贡献信誉服务接口
 * <p>
 * 信誉分 = 确定性加分 - 惩罚分
 * 等级: NEW(0) → CONTRIBUTOR(30) → ACTIVE(100) → TRUSTED(300) → EXPERT(800)
 * <p>
 * 事件类型:
 * - CORRECTION_ACCEPTED +15
 * - ANSWER_ACCEPTED +12
 * - REVIEW_HELPFUL +5
 * - POST_QUALITY +8
 * - QUESTION_ANSWERED +3
 * - ABUSE_PENALTY -20
 * - SPAM_REJECTED -10
 */
public interface ReputationService {

    /** 获取用户信誉（含等级、统计、最近事件） */
    ReputationVO getReputation(Long userId);

    /** 获取信誉事件历史（分页） */
    PageResult<ReputationEventVO> getEventHistory(Long userId, String eventType,
                                                   Integer pageNum, Integer pageSize);

    /** 记录信誉事件（幂等，eventKey去重） */
    void recordEvent(Long userId, String eventType, String eventKey,
                     String referenceType, Long referenceId);

    /** 重算用户信誉分（从user_reputation表统计重算） */
    void recalculate(Long userId);

    /** 批量重算（Admin） */
    void batchRecalculate(java.util.List<Long> userIds);

    /** 管理端: 按等级筛选用户列表 */
    PageResult<ReputationVO> adminListByLevel(String reputationLevel,
                                               Integer pageNum, Integer pageSize);

    /** 管理端: 手动调整信誉分 */
    void adminAdjustScore(Long userId, int delta, String reason);

    /** 每日贡献上限检查（防刷） */
    boolean checkDailyCap(Long userId);
}