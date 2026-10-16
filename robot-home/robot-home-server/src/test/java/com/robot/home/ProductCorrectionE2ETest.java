package com.robot.home;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.correction.dto.ParamCorrectionDTO;
import com.robot.home.correction.entity.RobotParamCorrection;
import com.robot.home.correction.mapper.RobotParamCorrectionMapper;
import com.robot.home.correction.service.RobotParamCorrectionService;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.entity.RobotParamDef;
import com.robot.home.robot.entity.RobotParamValue;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.robot.mapper.RobotParamDefMapper;
import com.robot.home.robot.mapper.RobotParamValueMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2E-03: 产品参数纠错全链路
 *
 * 验证流程: 提交纠错 → Admin审核 → 修改生效 → 原始数据保留
 * 安全验证: 审核拒绝不改变公开参数 / 并发审核不能重复生效 / 重复提交幂等保护
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDataSourceInitializer.class)
@Transactional
class ProductCorrectionE2ETest {

    private static final Long SUBMITTER_USER_ID = 920001L;
    private static final Long REVIEWER_USER_ID = 920002L;

    @Autowired
    private RobotParamCorrectionService correctionService;
    @Autowired
    private RobotParamCorrectionMapper correctionMapper;
    @Autowired
    private RobotMapper robotMapper;
    @Autowired
    private RobotParamDefMapper paramDefMapper;
    @Autowired
    private RobotParamValueMapper paramValueMapper;

    @MockBean
    private RedisUtils redisUtils;

    @Test
    void acceptedCorrectionUpdatesParamValueAndPreservesOriginal() {
        Long robotId = seedRobot();
        Long defId = seedParamDef("负载能力");
        seedParamValue(robotId, defId, "10kg");

        // 1. 提交纠错
        Long correctionId = correctionService.submit(SUBMITTER_USER_ID, correctionDto(robotId, defId, "15kg"));
        RobotParamCorrection record = correctionMapper.selectById(correctionId);
        assertEquals(0, record.getStatus(), "提交后应为待审核状态");
        assertEquals("10kg", record.getOldValue(), "原始值必须保留");
        assertEquals("15kg", record.getNewValue());

        // 2. 审核通过
        correctionService.audit(REVIEWER_USER_ID, correctionId, 1, "确认参数正确");

        // 3. 验证参数值已更新
        RobotParamValue updated = paramValueMapper.selectOne(Wrappers.<RobotParamValue>lambdaQuery()
                .eq(RobotParamValue::getRobotId, robotId)
                .eq(RobotParamValue::getDefId, defId));
        assertEquals("15kg", updated.getValue(), "审核通过后参数值应更新为新值");

        // 4. 验证纠错记录保留原始来源
        RobotParamCorrection audited = correctionMapper.selectById(correctionId);
        assertEquals(1, audited.getStatus());
        assertEquals("10kg", audited.getOldValue(), "审核后原始值仍须保留");
        assertEquals(REVIEWER_USER_ID, audited.getReviewerId());
        assertNotNull(audited.getReviewTime());
    }

    @Test
    void rejectedCorrectionDoesNotChangePublicParam() {
        Long robotId = seedRobot();
        Long defId = seedParamDef("工作半径");
        seedParamValue(robotId, defId, "1.5m");

        Long correctionId = correctionService.submit(SUBMITTER_USER_ID, correctionDto(robotId, defId, "2.0m"));
        correctionService.audit(REVIEWER_USER_ID, correctionId, 2, "证据不足");

        // 审核拒绝后参数值不变
        RobotParamValue unchanged = paramValueMapper.selectOne(Wrappers.<RobotParamValue>lambdaQuery()
                .eq(RobotParamValue::getRobotId, robotId)
                .eq(RobotParamValue::getDefId, defId));
        assertEquals("1.5m", unchanged.getValue(), "审核拒绝不应改变公开参数");
    }

    @Test
    void concurrentAuditCannotDoubleApply() {
        Long robotId = seedRobot();
        Long defId = seedParamDef("重复精度");
        seedParamValue(robotId, defId, "±0.02mm");

        Long correctionId = correctionService.submit(SUBMITTER_USER_ID, correctionDto(robotId, defId, "±0.01mm"));
        correctionService.audit(REVIEWER_USER_ID, correctionId, 1, "第一次审核");

        // 第二次审核必须被拒绝（并发审核不能重复生效）
        assertThrows(BusinessException.class, () ->
                correctionService.audit(REVIEWER_USER_ID + 1, correctionId, 1, "重复审核"));

        RobotParamValue value = paramValueMapper.selectOne(Wrappers.<RobotParamValue>lambdaQuery()
                .eq(RobotParamValue::getRobotId, robotId)
                .eq(RobotParamValue::getDefId, defId));
        assertEquals("±0.01mm", value.getValue(), "参数值只应被应用一次");
    }

    @Test
    void duplicatePendingSubmissionIsRejected() {
        Long robotId = seedRobot();
        Long defId = seedParamDef("防护等级");
        seedParamValue(robotId, defId, "IP54");

        // 第一次提交成功
        correctionService.submit(SUBMITTER_USER_ID, correctionDto(robotId, defId, "IP65"));

        // 同一用户同一参数的重复待审核提交必须被拒绝（幂等保护）
        assertThrows(BusinessException.class, () ->
                correctionService.submit(SUBMITTER_USER_ID, correctionDto(robotId, defId, "IP67")));

        // 只有一条纠错记录
        long count = correctionMapper.selectCount(Wrappers.<RobotParamCorrection>lambdaQuery()
                .eq(RobotParamCorrection::getRobotId, robotId)
                .eq(RobotParamCorrection::getDefId, defId)
                .eq(RobotParamCorrection::getUserId, SUBMITTER_USER_ID));
        assertEquals(1, count, "重复提交不应创建新记录");
    }

    @Test
    void correctionDoesNotOverwriteRobotEntitySourceData() {
        Long robotId = seedRobot();
        Long defId = seedParamDef("最大速度");
        seedParamValue(robotId, defId, "1.5m/s");

        Long correctionId = correctionService.submit(SUBMITTER_USER_ID, correctionDto(robotId, defId, "2.0m/s"));
        correctionService.audit(REVIEWER_USER_ID, correctionId, 1, "确认");

        // Robot实体的原始采集数据（dataSource/sourceUrl）不被覆盖
        Robot robot = robotMapper.selectById(robotId);
        assertEquals("crawler", robot.getDataSource(), "原始数据来源不被纠错覆盖");
        assertEquals("https://example.com/robot-spec", robot.getSourceUrl(), "原始来源URL保留");

        // 纠错记录保留完整审计轨迹
        RobotParamCorrection record = correctionMapper.selectById(correctionId);
        assertEquals("1.5m/s", record.getOldValue());
        assertEquals("2.0m/s", record.getNewValue());
    }

    // ==================== 辅助方法 ====================

    private Long seedRobot() {
        Robot robot = new Robot();
        robot.setName("E2E-03测试机器人");
        robot.setSlug("e2e03-test-robot-" + System.nanoTime());
        robot.setStatus(1);
        robot.setDataSource("crawler");
        robot.setSourceUrl("https://example.com/robot-spec");
        robot.setSourceName("测试来源");
        robotMapper.insert(robot);
        return robot.getId();
    }

    private Long seedParamDef(String name) {
        RobotParamDef def = new RobotParamDef();
        def.setGroupId(1L);
        def.setName(name);
        def.setUnit("");
        def.setType("text");
        def.setSort(1);
        paramDefMapper.insert(def);
        return def.getId();
    }

    private void seedParamValue(Long robotId, Long defId, String value) {
        RobotParamValue val = new RobotParamValue();
        val.setRobotId(robotId);
        val.setDefId(defId);
        val.setValue(value);
        paramValueMapper.insert(val);
    }

    private ParamCorrectionDTO correctionDto(Long robotId, Long defId, String newValue) {
        ParamCorrectionDTO dto = new ParamCorrectionDTO();
        dto.setRobotId(robotId);
        dto.setDefId(defId);
        dto.setNewValue(newValue);
        dto.setReason("E2E-03测试纠错");
        return dto;
    }
}