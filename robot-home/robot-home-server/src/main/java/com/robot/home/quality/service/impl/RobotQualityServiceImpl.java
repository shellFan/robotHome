package com.robot.home.quality.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.robot.home.common.PageResult;
import com.robot.home.common.util.PageUtils;
import com.robot.home.quality.entity.RobotQualityIssue;
import com.robot.home.quality.entity.RobotQualityScore;
import com.robot.home.quality.mapper.RobotQualityIssueMapper;
import com.robot.home.quality.mapper.RobotQualityScoreMapper;
import com.robot.home.quality.service.RobotQualityService;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobotQualityServiceImpl implements RobotQualityService {

    private final RobotQualityScoreMapper scoreMapper;
    private final RobotQualityIssueMapper issueMapper;
    private final RobotMapper robotMapper;

    @Override
    @Transactional
    public RobotQualityScore computeAndSave(Long robotId) {
        Robot robot = robotMapper.selectById(robotId);
        if (robot == null) {
            return null;
        }

        // 13维度评分计算
        int basicInfoScore = computeBasicInfo(robot);
        int paramScore = computeParams(robot);
        int imageScore = computeImages(robot);
        int videoScore = computeVideo(robot);
        int docScore = computeDoc(robot);
        int priceScore = computePrice(robot);
        int specScore = computeSpec(robot);
        int contactScore = computeContact(robot);
        int brandScore = computeBrand(robot);
        int categoryScore = computeCategory(robot);
        int descScore = computeDesc(robot);
        int qaScore = 0; // Q&A维度需关联查询，暂置0
        int reviewScore = 0; // Review维度需关联查询，暂置0

        int totalScore = basicInfoScore + paramScore + imageScore + videoScore
                + docScore + priceScore + specScore + contactScore + brandScore
                + categoryScore + descScore + qaScore + reviewScore;

        // Upsert score
        RobotQualityScore existing = scoreMapper.selectOne(
                new LambdaQueryWrapper<RobotQualityScore>().eq(RobotQualityScore::getRobotId, robotId));
        RobotQualityScore score;
        if (existing != null) {
            score = existing;
            score.setBasicInfoScore(basicInfoScore);
            score.setParamScore(paramScore);
            score.setImageScore(imageScore);
            score.setVideoScore(videoScore);
            score.setDocScore(docScore);
            score.setPriceScore(priceScore);
            score.setSpecScore(specScore);
            score.setContactScore(contactScore);
            score.setBrandScore(brandScore);
            score.setCategoryScore(categoryScore);
            score.setDescScore(descScore);
            score.setQaScore(qaScore);
            score.setReviewScore(reviewScore);
            score.setTotalScore(totalScore);
            score.setUpdateTime(LocalDateTime.now());
            scoreMapper.updateById(score);
        } else {
            score = new RobotQualityScore();
            score.setRobotId(robotId);
            score.setBasicInfoScore(basicInfoScore);
            score.setParamScore(paramScore);
            score.setImageScore(imageScore);
            score.setVideoScore(videoScore);
            score.setDocScore(docScore);
            score.setPriceScore(priceScore);
            score.setSpecScore(specScore);
            score.setContactScore(contactScore);
            score.setBrandScore(brandScore);
            score.setCategoryScore(categoryScore);
            score.setDescScore(descScore);
            score.setQaScore(qaScore);
            score.setReviewScore(reviewScore);
            score.setTotalScore(totalScore);
            score.setCreateTime(LocalDateTime.now());
            score.setUpdateTime(LocalDateTime.now());
            try {
                scoreMapper.insert(score);
            } catch (DuplicateKeyException e) {
                // 并发插入冲突，回退为更新
                log.debug("Concurrent insert for robotId={}, falling back to update", robotId);
                RobotQualityScore concurrent = scoreMapper.selectOne(
                        new LambdaQueryWrapper<RobotQualityScore>().eq(RobotQualityScore::getRobotId, robotId));
                if (concurrent != null) {
                    score = concurrent;
                    score.setBasicInfoScore(basicInfoScore);
                    score.setParamScore(paramScore);
                    score.setImageScore(imageScore);
                    score.setVideoScore(videoScore);
                    score.setDocScore(docScore);
                    score.setPriceScore(priceScore);
                    score.setSpecScore(specScore);
                    score.setContactScore(contactScore);
                    score.setBrandScore(brandScore);
                    score.setCategoryScore(categoryScore);
                    score.setDescScore(descScore);
                    score.setQaScore(qaScore);
                    score.setReviewScore(reviewScore);
                    score.setTotalScore(totalScore);
                    score.setUpdateTime(LocalDateTime.now());
                    scoreMapper.updateById(score);
                }
            }
        }

        // 生成质量问题
        generateIssues(robotId, score);

        return score;
    }

    @Override
    public RobotQualityScore getScore(Long robotId) {
        return scoreMapper.selectOne(
                new LambdaQueryWrapper<RobotQualityScore>().eq(RobotQualityScore::getRobotId, robotId));
    }

    @Override
    public List<RobotQualityIssue> getIssues(Long robotId) {
        return issueMapper.selectList(
                new LambdaQueryWrapper<RobotQualityIssue>()
                        .eq(RobotQualityIssue::getRobotId, robotId)
                        .eq(RobotQualityIssue::getStatus, 0)
                        .orderByAsc(RobotQualityIssue::getSeverity));
    }

    @Override
    public PageResult<RobotQualityScore> adminListScores(Integer pageNum, Integer pageSize) {
        pageNum = PageUtils.normalizePageNum(pageNum);
        pageSize = PageUtils.normalizePageSize(pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<RobotQualityScore> scorePage = scoreMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<RobotQualityScore>().orderByAsc(RobotQualityScore::getTotalScore));
        return new PageResult<>(pageNum, pageSize, scorePage.getTotal(), scorePage.getRecords());
    }

    @Override
    public PageResult<RobotQualityIssue> adminListIssues(Integer pageNum, Integer pageSize, Integer status) {
        pageNum = PageUtils.normalizePageNum(pageNum);
        pageSize = PageUtils.normalizePageSize(pageSize);
        LambdaQueryWrapper<RobotQualityIssue> wrapper = new LambdaQueryWrapper<RobotQualityIssue>()
                .orderByAsc(RobotQualityIssue::getSeverity);
        if (status != null) {
            wrapper.eq(RobotQualityIssue::getStatus, status);
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<RobotQualityIssue> issuePage = issueMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(pageNum, pageSize, issuePage.getTotal(), issuePage.getRecords());
    }

    @Override
    @Transactional
    public void updateIssueStatus(Long id, Integer status) {
        issueMapper.update(null, new LambdaUpdateWrapper<RobotQualityIssue>()
                .eq(RobotQualityIssue::getId, id)
                .set(RobotQualityIssue::getStatus, status)
                .set(RobotQualityIssue::getUpdateTime, LocalDateTime.now()));
    }

    // ---- 评分维度 ----

    private int computeBasicInfo(Robot r) {
        int s = 0;
        if (r.getName() != null && !r.getName().trim().isEmpty()) s += 5;
        if (r.getSubtitle() != null && !r.getSubtitle().trim().isEmpty()) s += 3;
        if (r.getModel() != null && !r.getModel().trim().isEmpty()) s += 2;
        return s; // max 10
    }

    private int computeParams(Robot r) {
        // mainParams非空即有参数
        if (r.getMainParams() != null && !r.getMainParams().trim().isEmpty()) return 10;
        return 0;
    }

    private int computeImages(Robot r) {
        // images字段存储图片列表
        if (r.getImages() == null || r.getImages().trim().isEmpty()) return 0;
        // 估算图片数量(逗号或JSON数组分隔)
        int count = r.getImages().split(",").length;
        if (count >= 5) return 10;
        return count * 2; // max 10
    }

    private int computeVideo(Robot r) {
        if (r.getVideoCount() == null || r.getVideoCount() == 0) return 0;
        return r.getVideoCount() >= 2 ? 10 : 5;
    }

    private int computeDoc(Robot r) {
        // detail字段为图文详情(富文本)
        if (r.getDetail() == null || r.getDetail().trim().isEmpty()) return 0;
        int len = r.getDetail().trim().length();
        if (len >= 500) return 10;
        if (len >= 100) return 5;
        return 2;
    }

    private int computePrice(Robot r) {
        if (r.getGuidePrice() != null && r.getGuidePrice().doubleValue() > 0) return 10;
        if (r.getMarketPrice() != null && r.getMarketPrice().doubleValue() > 0) return 5;
        return 0;
    }

    private int computeSpec(Robot r) {
        // 核心规格: weight+payload+maxSpeed+batteryLife
        int s = 0;
        if (r.getWeight() != null) s += 3;
        if (r.getPayload() != null) s += 3;
        if (r.getMaxSpeed() != null) s += 2;
        if (r.getBatteryLife() != null) s += 2;
        return s; // max 10
    }

    private int computeContact(Robot r) {
        // 暂无contactInfo字段，基于sourceName推断
        if (r.getSourceName() != null && !r.getSourceName().trim().isEmpty()) return 5;
        return 0;
    }

    private int computeBrand(Robot r) {
        if (r.getBrandId() != null) return 10;
        return 0;
    }

    private int computeCategory(Robot r) {
        if (r.getCategoryId() != null) return 10;
        return 0;
    }

    private int computeDesc(Robot r) {
        // subtitle作为简要描述
        if (r.getSubtitle() == null || r.getSubtitle().trim().isEmpty()) return 0;
        int len = r.getSubtitle().trim().length();
        if (len >= 50) return 10;
        if (len >= 20) return 5;
        return 2;
    }

    // ---- Issue生成 ----

    private void generateIssues(Long robotId, RobotQualityScore score) {
        // 删除未处理(状态=0)的旧issues，保留已处理/已忽略的
        issueMapper.delete(new LambdaQueryWrapper<RobotQualityIssue>()
                .eq(RobotQualityIssue::getRobotId, robotId)
                .eq(RobotQualityIssue::getStatus, 0));

        List<RobotQualityIssue> issues = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (score.getBasicInfoScore() < 5) {
            issues.add(buildIssue(robotId, "MISSING_BASIC_INFO", "基本信息不完整", 1, now));
        }
        if (score.getImageScore() < 5) {
            issues.add(buildIssue(robotId, "MISSING_IMAGES", "图片不足(建议至少5张)", 1, now));
        }
        if (score.getVideoScore() == 0) {
            issues.add(buildIssue(robotId, "MISSING_VIDEO", "缺少视频介绍", 2, now));
        }
        if (score.getPriceScore() == 0) {
            issues.add(buildIssue(robotId, "MISSING_PRICE", "缺少价格信息", 1, now));
        }
        if (score.getParamScore() == 0) {
            issues.add(buildIssue(robotId, "MISSING_PARAMS", "缺少参数数据", 1, now));
        }
        if (score.getContactScore() == 0) {
            issues.add(buildIssue(robotId, "MISSING_CONTACT", "缺少联系方式", 2, now));
        }
        if (score.getCategoryScore() == 0) {
            issues.add(buildIssue(robotId, "MISSING_CATEGORY", "未分类", 2, now));
        }
        if (score.getDescScore() < 5) {
            issues.add(buildIssue(robotId, "LOW_QUALITY_DESC", "描述内容过短", 3, now));
        }

        for (RobotQualityIssue issue : issues) {
            issueMapper.insert(issue);
        }
    }

    private RobotQualityIssue buildIssue(Long robotId, String type, String description, int severity, LocalDateTime now) {
        RobotQualityIssue issue = new RobotQualityIssue();
        issue.setRobotId(robotId);
        issue.setIssueType(type);
        issue.setDescription(description);
        issue.setSeverity(severity);
        issue.setStatus(0);
        issue.setCreateTime(now);
        issue.setUpdateTime(now);
        return issue;
    }
}