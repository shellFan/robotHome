package com.robot.home.correction.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.common.PageResult;
import com.robot.home.common.Constants;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.XssUtils;
import com.robot.home.correction.dto.ParamCorrectionDTO;
import com.robot.home.correction.entity.RobotParamCorrection;
import com.robot.home.correction.mapper.RobotParamCorrectionMapper;
import com.robot.home.correction.service.RobotParamCorrectionService;
import com.robot.home.correction.vo.ParamCorrectionVO;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.entity.RobotParamDef;
import com.robot.home.robot.entity.RobotParamValue;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.robot.mapper.RobotParamDefMapper;
import com.robot.home.robot.mapper.RobotParamValueMapper;
import com.robot.home.user.entity.User;
import com.robot.home.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 参数纠错服务实现
 */
@Service
public class RobotParamCorrectionServiceImpl extends ServiceImpl<RobotParamCorrectionMapper, RobotParamCorrection>
        implements RobotParamCorrectionService {

    private static final Logger log = LoggerFactory.getLogger(RobotParamCorrectionServiceImpl.class);

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_ACCEPTED = 1;
    private static final int STATUS_REJECTED = 2;

    @Resource
    private RobotMapper robotMapper;
    @Resource
    private RobotParamDefMapper paramDefMapper;
    @Resource
    private RobotParamValueMapper paramValueMapper;
    @Resource
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, ParamCorrectionDTO dto) {
        // 1. 校验机器人存在
        Robot robot = robotMapper.selectById(dto.getRobotId());
        if (robot == null) {
            throw new BusinessException("机器人不存在");
        }

        // 2. 校验参数定义存在
        RobotParamDef paramDef = paramDefMapper.selectById(dto.getDefId());
        if (paramDef == null) {
            throw new BusinessException("参数定义不存在");
        }

        // 3. 查询当前参数值
        RobotParamValue paramValue = paramValueMapper.selectOne(
                Wrappers.<RobotParamValue>lambdaQuery()
                        .eq(RobotParamValue::getRobotId, dto.getRobotId())
                        .eq(RobotParamValue::getDefId, dto.getDefId()));
        String oldValue = paramValue != null ? paramValue.getValue() : "";

        // 4. 防重复：同一用户同一参数同一新值的待审核纠错
        Long existCount = count(Wrappers.<RobotParamCorrection>lambdaQuery()
                .eq(RobotParamCorrection::getRobotId, dto.getRobotId())
                .eq(RobotParamCorrection::getDefId, dto.getDefId())
                .eq(RobotParamCorrection::getUserId, userId)
                .eq(RobotParamCorrection::getStatus, STATUS_PENDING)
                .eq(RobotParamCorrection::getDeleted, Constants.DELETED_NO));
        if (existCount > 0) {
            throw new BusinessException("您已有该参数的待审核纠错，请等待审核");
        }

        // 5. 构建纠错记录
        RobotParamCorrection correction = new RobotParamCorrection();
        correction.setRobotId(dto.getRobotId());
        correction.setUserId(userId);
        correction.setDefId(dto.getDefId());
        correction.setParamName(paramDef.getName());
        correction.setOldValue(oldValue);
        correction.setNewValue(XssUtils.escapeText(StrUtil.trim(dto.getNewValue())));
        correction.setReason(XssUtils.escapeText(StrUtil.trim(dto.getReason())));
        correction.setStatus(STATUS_PENDING);
        save(correction);

        return correction.getId();
    }

    @Override
    public PageResult<ParamCorrectionVO> myCorrections(Long userId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<RobotParamCorrection> page = new Page<>(pn, ps);
        IPage<RobotParamCorrection> result = page(page, Wrappers.<RobotParamCorrection>lambdaQuery()
                .eq(RobotParamCorrection::getUserId, userId)
                .eq(RobotParamCorrection::getDeleted, Constants.DELETED_NO)
                .orderByDesc(RobotParamCorrection::getCreateTime));
        List<ParamCorrectionVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), voList);
    }

    // ---- 管理端 ----

    @Override
    public PageResult<ParamCorrectionVO> adminPage(Integer status, Long robotId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<RobotParamCorrection> page = new Page<>(pn, ps);
        IPage<RobotParamCorrection> result = page(page, Wrappers.<RobotParamCorrection>lambdaQuery()
                .eq(status != null, RobotParamCorrection::getStatus, status)
                .eq(robotId != null, RobotParamCorrection::getRobotId, robotId)
                .eq(RobotParamCorrection::getDeleted, Constants.DELETED_NO)
                .orderByAsc(RobotParamCorrection::getStatus)  // 待审核排前面
                .orderByDesc(RobotParamCorrection::getCreateTime));
        List<ParamCorrectionVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long reviewerId, Long correctionId, Integer status, String reviewNote) {
        if (status != STATUS_ACCEPTED && status != STATUS_REJECTED) {
            throw new BusinessException("审核状态无效");
        }
        RobotParamCorrection correction = getById(correctionId);
        if (correction == null || correction.getDeleted() == Constants.DELETED_YES) {
            throw new BusinessException("纠错记录不存在");
        }
        if (correction.getStatus() != STATUS_PENDING) {
            throw new BusinessException("该纠错已审核");
        }

        // 更新审核状态
        update(Wrappers.<RobotParamCorrection>lambdaUpdate()
                .eq(RobotParamCorrection::getId, correctionId)
                .set(RobotParamCorrection::getStatus, status)
                .set(RobotParamCorrection::getReviewerId, reviewerId)
                .set(RobotParamCorrection::getReviewNote, StrUtil.isNotBlank(reviewNote) ? XssUtils.escapeText(reviewNote) : null));

        // 采纳时自动更新RobotParamValue
        if (status == STATUS_ACCEPTED) {
            applyCorrection(correction);
        }
    }

    /**
     * 采纳纠错：更新RobotParamValue
     */
    private void applyCorrection(RobotParamCorrection correction) {
        try {
            RobotParamValue paramValue = paramValueMapper.selectOne(
                    Wrappers.<RobotParamValue>lambdaQuery()
                            .eq(RobotParamValue::getRobotId, correction.getRobotId())
                            .eq(RobotParamValue::getDefId, correction.getDefId()));

            if (paramValue != null) {
                // 更新已有参数值
                paramValueMapper.update(null, Wrappers.<RobotParamValue>lambdaUpdate()
                        .eq(RobotParamValue::getId, paramValue.getId())
                        .set(RobotParamValue::getValue, correction.getNewValue()));
            } else {
                // 新增参数值
                RobotParamValue newValue = new RobotParamValue();
                newValue.setRobotId(correction.getRobotId());
                newValue.setDefId(correction.getDefId());
                newValue.setValue(correction.getNewValue());
                paramValueMapper.insert(newValue);
            }
            log.info("参数纠错已采纳并更新: robotId={}, defId={}, oldValue={}, newValue={}",
                    correction.getRobotId(), correction.getDefId(), correction.getOldValue(), correction.getNewValue());
        } catch (Exception e) {
            log.error("采纳纠错更新参数值失败: correctionId={}", correction.getId(), e);
            throw new BusinessException("采纳纠错失败，请重试");
        }
    }

    /**
     * Entity -> VO
     */
    private ParamCorrectionVO toVO(RobotParamCorrection c) {
        ParamCorrectionVO vo = new ParamCorrectionVO();
        vo.setId(c.getId());
        vo.setRobotId(c.getRobotId());
        vo.setDefId(c.getDefId());
        vo.setParamName(c.getParamName());
        vo.setOldValue(c.getOldValue());
        vo.setNewValue(c.getNewValue());
        vo.setReason(c.getReason());
        vo.setStatus(c.getStatus());
        vo.setReviewerId(c.getReviewerId());
        vo.setReviewNote(c.getReviewNote());
        vo.setCreateTime(c.getCreateTime());
        vo.setUpdateTime(c.getUpdateTime());
        vo.setUserId(c.getUserId());

        // 机器人名称
        if (c.getRobotId() != null) {
            Robot robot = robotMapper.selectById(c.getRobotId());
            if (robot != null) {
                vo.setRobotName(robot.getName());
            }
        }

        // 用户昵称
        if (c.getUserId() != null) {
            User user = userMapper.selectById(c.getUserId());
            if (user != null) {
                vo.setUserNickname(user.getNickname());
            }
        }

        return vo;
    }
}