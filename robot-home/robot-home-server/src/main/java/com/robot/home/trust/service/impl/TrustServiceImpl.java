package com.robot.home.trust.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.common.Constants;
import com.robot.home.common.PageResult;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.correction.entity.RobotParamCorrection;
import com.robot.home.correction.mapper.RobotParamCorrectionMapper;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.trust.entity.RobotChangeRecord;
import com.robot.home.trust.entity.RobotDataSource;
import com.robot.home.trust.mapper.RobotChangeRecordMapper;
import com.robot.home.trust.mapper.RobotDataSourceMapper;
import com.robot.home.trust.service.TrustService;
import com.robot.home.trust.vo.RobotChangeRecordVO;
import com.robot.home.trust.vo.RobotDataSourceVO;
import com.robot.home.trust.vo.RobotTrustVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 信任体系服务实现
 * <p>
 * Trust计算逻辑:
 * - 基础分 = 40
 * - 来源加分: OFFICIAL+15, VENDOR_SITE+10, TRUSTED_MEDIA+8, MANUAL+7, USER_CORRECTION+5, CRAWLER+3
 * - 参数完整度: mainParams+5, weight+3, payload+3, batteryLife+3, protectionLevel+2
 * - 纠错加分: 无待处理+5, 有待处理-10
 * - 验证加分: lastVerifiedTime 30天内+10, 60天内+5
 * - 封顶100, 最低0
 * - 等级: >=80 VERIFIED, >=60 HIGH, >=30 NORMAL, <30 LOW
 * <p>
 * Redis缓存: key=robot:trust:{robotId}, TTL=300s, 失败降级到DB
 */
@Service
public class TrustServiceImpl implements TrustService {

    private static final Logger log = LoggerFactory.getLogger(TrustServiceImpl.class);

    private static final long CACHE_SECONDS = 300L;

    /** 来源类型 → 加分权重 */
    private static final java.util.Map<String, Integer> SOURCE_WEIGHT_MAP = new java.util.HashMap<>();
    static {
        SOURCE_WEIGHT_MAP.put(Constants.SOURCE_OFFICIAL, 15);
        SOURCE_WEIGHT_MAP.put(Constants.SOURCE_VENDOR_SITE, 10);
        SOURCE_WEIGHT_MAP.put(Constants.SOURCE_TRUSTED_MEDIA, 8);
        SOURCE_WEIGHT_MAP.put(Constants.SOURCE_MANUAL, 7);
        SOURCE_WEIGHT_MAP.put(Constants.SOURCE_USER_CORRECTION, 5);
        SOURCE_WEIGHT_MAP.put(Constants.SOURCE_CRAWLER, 3);
    }

    /** 变更记录白名单字段（公开可见） */
    private static final Set<String> PUBLIC_CHANGE_FIELDS = new HashSet<>(Arrays.asList(
            "guide_price", "market_price", "weight", "payload", "max_speed",
            "battery_life", "operating_temp", "protection_level",
            "name", "model", "subtitle", "status", "release_date"
    ));

    @Resource
    private RobotMapper robotMapper;
    @Resource
    private RobotDataSourceMapper dataSourceMapper;
    @Resource
    private RobotChangeRecordMapper changeRecordMapper;
    @Resource
    private RobotParamCorrectionMapper correctionMapper;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public RobotTrustVO getTrust(Long robotId) {
        String cacheKey = Constants.CACHE_TRUST_PREFIX + robotId;
        try {
            RobotTrustVO cached = redisUtils.getObj(cacheKey, RobotTrustVO.class);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis信任缓存读取失败，降级到DB: robotId={}, error={}", robotId, e.getMessage());
        }

        RobotTrustVO vo = calculateTrust(robotId);

        try {
            redisUtils.setObj(cacheKey, vo, CACHE_SECONDS + (long) (Math.random() * 30), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis信任缓存写入失败（不影响返回）: robotId={}, error={}", robotId, e.getMessage());
        }

        return vo;
    }

    @Override
    public List<RobotDataSourceVO> getDataSources(Long robotId) {
        List<RobotDataSource> sources = dataSourceMapper.selectList(
                Wrappers.<RobotDataSource>lambdaQuery()
                        .eq(RobotDataSource::getRobotId, robotId)
                        .orderByDesc(RobotDataSource::getTrustWeight));
        return sources.stream().map(this::toDataSourceVO).collect(Collectors.toList());
    }

    @Override
    public PageResult<RobotChangeRecordVO> getChangeHistory(Long robotId, String changeType,
                                                             Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<RobotChangeRecord> page = new Page<>(pn, ps);

        IPage<RobotChangeRecord> result = changeRecordMapper.selectPage(page,
                Wrappers.<RobotChangeRecord>lambdaQuery()
                        .eq(RobotChangeRecord::getRobotId, robotId)
                        .eq(StrUtil.isNotBlank(changeType), RobotChangeRecord::getChangeType, changeType)
                        .in(RobotChangeRecord::getFieldName, PUBLIC_CHANGE_FIELDS)
                        .orderByDesc(RobotChangeRecord::getChangeTime));

        List<RobotChangeRecordVO> voList = result.getRecords().stream()
                .map(this::toChangeRecordVO)
                .collect(Collectors.toList());
        return PageResult.of(pn, ps, result.getTotal(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculateTrust(Long robotId) {
        RobotTrustVO vo = calculateTrust(robotId);

        // 更新robot表的trust字段
        Robot update = new Robot();
        update.setId(robotId);
        update.setTrustLevel(vo.getLevel());
        update.setTrustScore(vo.getScore());
        update.setPendingCorrectionCount(vo.getPendingCorrectionCount());
        robotMapper.updateById(update);

        // 清除缓存，下次读取时重新加载
        try {
            redisUtils.delete(Constants.CACHE_TRUST_PREFIX + robotId);
        } catch (Exception e) {
            log.warn("Redis信任缓存删除失败: robotId={}, error={}", robotId, e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDataSource(Long robotId, String sourceType, String sourceName, String sourceUrl) {
        Integer weight = SOURCE_WEIGHT_MAP.get(sourceType);
        if (weight == null) {
            weight = 3;
        }

        RobotDataSource ds = new RobotDataSource();
        ds.setRobotId(robotId);
        ds.setSourceType(sourceType);
        ds.setSourceName(sourceName);
        ds.setSourceUrl(sourceUrl);
        ds.setTrustWeight(weight);
        ds.setVerified(0);
        dataSourceMapper.insert(ds);

        // 添加来源后重算信任分
        recalculateTrust(robotId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordChange(Long robotId, String changeType, String fieldName, String fieldLabel,
                              String oldValue, String newValue, String sourceType) {
        LocalDateTime now = LocalDateTime.now();
        String eventKey = generateEventKey(robotId, changeType, fieldName, now);

        // 幂等: 先删除同eventKey的旧记录，再插入（delete-first模式）
        changeRecordMapper.delete(Wrappers.<RobotChangeRecord>lambdaQuery()
                .eq(RobotChangeRecord::getEventKey, eventKey));

        RobotChangeRecord record = new RobotChangeRecord();
        record.setRobotId(robotId);
        record.setChangeType(changeType);
        record.setFieldName(fieldName);
        record.setFieldLabel(fieldLabel);
        record.setOldValue(oldValue);
        record.setNewValue(newValue);
        record.setSourceType(sourceType);
        record.setSourceName(null);
        record.setVerified(0);
        record.setEventKey(eventKey);
        record.setChangeTime(now);

        try {
            changeRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发写入，幂等返回
            log.info("变更记录并发写入，幂等忽略: eventKey={}", eventKey);
        }

        // 清除信任缓存（变更可能影响信任分）
        try {
            redisUtils.delete(Constants.CACHE_TRUST_PREFIX + robotId);
        } catch (Exception e) {
            log.warn("Redis信任缓存删除失败: robotId={}, error={}", robotId, e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 核心信任分计算
     */
    private RobotTrustVO calculateTrust(Long robotId) {
        Robot robot = robotMapper.selectById(robotId);
        if (robot == null) {
            return buildEmptyTrust();
        }

        List<String> reasons = new ArrayList<>();
        int score = 40; // 基础分
        reasons.add("基础分40");

        // 1. 来源加分
        List<RobotDataSource> sources = dataSourceMapper.selectList(
                Wrappers.<RobotDataSource>lambdaQuery()
                        .eq(RobotDataSource::getRobotId, robotId));
        for (RobotDataSource ds : sources) {
            Integer w = SOURCE_WEIGHT_MAP.get(ds.getSourceType());
            if (w != null) {
                score += w;
                reasons.add(ds.getSourceType() + "来源+" + w);
            }
        }

        // 2. 参数完整度加分
        if (StrUtil.isNotBlank(robot.getMainParams())) {
            score += 5;
            reasons.add("主要参数完整+5");
        }
        if (robot.getWeight() != null && robot.getWeight().compareTo(BigDecimal.ZERO) > 0) {
            score += 3;
            reasons.add("重量参数+3");
        }
        if (robot.getPayload() != null && robot.getPayload().compareTo(BigDecimal.ZERO) > 0) {
            score += 3;
            reasons.add("负载参数+3");
        }
        if (robot.getBatteryLife() != null && robot.getBatteryLife().compareTo(BigDecimal.ZERO) > 0) {
            score += 3;
            reasons.add("续航参数+3");
        }
        if (StrUtil.isNotBlank(robot.getProtectionLevel())) {
            score += 2;
            reasons.add("防护等级+2");
        }

        // 3. 纠错加分
        int pendingCount = countPendingCorrections(robotId);
        if (pendingCount == 0) {
            score += 5;
            reasons.add("无待处理纠错+5");
        } else {
            score -= 10;
            reasons.add("有待处理纠错-10");
        }

        // 4. 验证加分
        if (robot.getLastVerifiedTime() != null) {
            long days = ChronoUnit.DAYS.between(robot.getLastVerifiedTime(), LocalDateTime.now());
            if (days <= 30) {
                score += 10;
                reasons.add("30天内验证+10");
            } else if (days <= 60) {
                score += 5;
                reasons.add("60天内验证+5");
            }
        }

        // 封顶100，最低0
        score = Math.max(0, Math.min(100, score));

        // 等级
        String level;
        if (score >= 80) {
            level = Constants.TRUST_VERIFIED;
        } else if (score >= 60) {
            level = Constants.TRUST_HIGH;
        } else if (score >= 30) {
            level = Constants.TRUST_NORMAL;
        } else {
            level = Constants.TRUST_LOW;
        }

        RobotTrustVO vo = new RobotTrustVO();
        vo.setLevel(level);
        vo.setScore(score);
        vo.setReasonList(reasons);
        vo.setLastVerifiedTime(robot.getLastVerifiedTime());
        vo.setSourceCount(sources.size());
        vo.setPendingCorrectionCount(pendingCount);
        return vo;
    }

    /**
     * 统计待处理纠错数
     */
    private int countPendingCorrections(Long robotId) {
        Long count = correctionMapper.selectCount(
                Wrappers.<RobotParamCorrection>lambdaQuery()
                        .eq(RobotParamCorrection::getRobotId, robotId)
                        .eq(RobotParamCorrection::getStatus, 0));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 生成事件唯一键: SHA-256(robotId + changeType + fieldName + 日期部分)
     */
    private String generateEventKey(Long robotId, String changeType, String fieldName, LocalDateTime changeTime) {
        String raw = robotId + changeType + fieldName + changeTime.toLocalDate().toString();
        return SecureUtil.sha256(raw);
    }

    private RobotTrustVO buildEmptyTrust() {
        RobotTrustVO vo = new RobotTrustVO();
        vo.setLevel(Constants.TRUST_LOW);
        vo.setScore(0);
        vo.setReasonList(new ArrayList<>());
        vo.setSourceCount(0);
        vo.setPendingCorrectionCount(0);
        return vo;
    }

    private RobotDataSourceVO toDataSourceVO(RobotDataSource ds) {
        RobotDataSourceVO vo = new RobotDataSourceVO();
        vo.setId(ds.getId());
        vo.setSourceType(ds.getSourceType());
        vo.setSourceName(ds.getSourceName());
        vo.setSourceUrl(ds.getSourceUrl());
        vo.setTrustWeight(ds.getTrustWeight());
        vo.setVerified(ds.getVerified());
        vo.setVerifiedTime(ds.getVerifiedTime());
        return vo;
    }

    private RobotChangeRecordVO toChangeRecordVO(RobotChangeRecord r) {
        RobotChangeRecordVO vo = new RobotChangeRecordVO();
        vo.setId(r.getId());
        vo.setRobotId(r.getRobotId());
        vo.setChangeType(r.getChangeType());
        vo.setFieldName(r.getFieldName());
        vo.setFieldLabel(r.getFieldLabel());
        vo.setOldValue(r.getOldValue());
        vo.setNewValue(r.getNewValue());
        vo.setSourceType(r.getSourceType());
        vo.setSourceName(r.getSourceName());
        vo.setVerified(r.getVerified());
        vo.setChangeTime(r.getChangeTime());
        return vo;
    }
}