package com.robot.home.growth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.community.mapper.CommunityPostMapper;
import com.robot.home.community.entity.CommunityPost;
import com.robot.home.correction.mapper.RobotParamCorrectionMapper;
import com.robot.home.correction.entity.RobotParamCorrection;
import com.robot.home.growth.entity.GrowthDailyStat;
import com.robot.home.growth.mapper.GrowthDailyStatMapper;
import com.robot.home.inquiry.mapper.InquiryMapper;
import com.robot.home.inquiry.entity.Inquiry;
import com.robot.home.message.mapper.MessageMapper;
import com.robot.home.message.entity.Message;
import com.robot.home.qa.mapper.RobotAnswerMapper;
import com.robot.home.qa.mapper.RobotQuestionMapper;
import com.robot.home.qa.entity.RobotAnswer;
import com.robot.home.qa.entity.RobotQuestion;
import com.robot.home.reputation.mapper.ReputationEventMapper;
import com.robot.home.review.mapper.RobotReviewMapper;
import com.robot.home.review.entity.RobotReview;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.subscription.mapper.UserSubscriptionMapper;
import com.robot.home.subscription.entity.UserSubscription;
import com.robot.home.user.mapper.UserMapper;
import com.robot.home.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 定时任务：每日增长统计快照
 * 每天凌晨2:00执行，聚合前一天的业务数据到growth_daily_stat表
 * 供GrowthDashboard和EcosystemDashboard读取
 */
@Slf4j
@Component
@EnableScheduling
public class GrowthDailyStatScheduler {

    @Resource
    private GrowthDailyStatMapper growthDailyStatMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private RobotMapper robotMapper;
    @Resource
    private RobotQuestionMapper questionMapper;
    @Resource
    private RobotAnswerMapper answerMapper;
    @Resource
    private CommunityPostMapper postMapper;
    @Resource
    private RobotReviewMapper reviewMapper;
    @Resource
    private InquiryMapper inquiryMapper;
    @Resource
    private UserSubscriptionMapper subscriptionMapper;
    @Resource
    private MessageMapper messageMapper;
    @Resource
    private ReputationEventMapper reputationEventMapper;
    @Resource
    private RobotParamCorrectionMapper correctionMapper;

    /**
     * 每天凌晨2:00执行，统计前一天数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyStat() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime dayStart = yesterday.atStartOfDay();
        LocalDateTime dayEnd = yesterday.atTime(LocalTime.MAX);

        log.info("开始生成增长日统计: statDate={}", yesterday);
        try {
            GrowthDailyStat stat = buildStat(yesterday, dayStart, dayEnd);
            saveStat(stat);
            log.info("增长日统计完成: statDate={}, newUsers={}, inquiries={}, notificationSent={}",
                    yesterday, stat.getNewUsers(), stat.getInquiries(), stat.getNotificationSent());
        } catch (Exception e) {
            log.error("增长日统计失败: statDate={}, error={}", yesterday, e.getMessage(), e);
        }
    }

    /**
     * 首次启动时，如果今天没有数据，立即填充
     */
    @Scheduled(initialDelay = 60 * 1000L, fixedDelay = Long.MAX_VALUE)
    public void initTodayStat() {
        LocalDate today = LocalDate.now();
        Long exist = growthDailyStatMapper.selectCount(Wrappers.<GrowthDailyStat>lambdaQuery()
                .eq(GrowthDailyStat::getStatDate, today));
        if (exist == null || exist == 0) {
            log.info("今日增长统计不存在，立即填充: statDate={}", today);
            try {
                LocalDateTime dayStart = today.atStartOfDay();
                LocalDateTime dayEnd = LocalDateTime.now();
                GrowthDailyStat stat = buildStat(today, dayStart, dayEnd);
                saveStat(stat);
                log.info("今日增长统计填充完成");
            } catch (Exception e) {
                log.warn("今日增长统计填充失败: error={}", e.getMessage());
            }
        }
    }

    private GrowthDailyStat buildStat(LocalDate statDate, LocalDateTime dayStart, LocalDateTime dayEnd) {
        GrowthDailyStat stat = new GrowthDailyStat();
        stat.setStatDate(statDate);

        // 用户统计
        stat.setNewUsers(Math.toIntExact(userMapper.selectCount(
                Wrappers.<User>lambdaQuery().between(User::getCreateTime, dayStart, dayEnd))));

        // 内容统计
        stat.setQuestions(Math.toIntExact(questionMapper.selectCount(
                Wrappers.<RobotQuestion>lambdaQuery().between(RobotQuestion::getCreateTime, dayStart, dayEnd))));
        stat.setAnswers(Math.toIntExact(answerMapper.selectCount(
                Wrappers.<RobotAnswer>lambdaQuery().between(RobotAnswer::getCreateTime, dayStart, dayEnd))));
        stat.setPosts(Math.toIntExact(postMapper.selectCount(
                Wrappers.<CommunityPost>lambdaQuery().between(CommunityPost::getCreateTime, dayStart, dayEnd))));
        stat.setReviews(Math.toIntExact(reviewMapper.selectCount(
                Wrappers.<RobotReview>lambdaQuery().between(RobotReview::getCreateTime, dayStart, dayEnd))));

        // 采购统计
        stat.setInquiries(Math.toIntExact(inquiryMapper.selectCount(
                Wrappers.<Inquiry>lambdaQuery().between(Inquiry::getCreateTime, dayStart, dayEnd))));

        // 订阅统计
        stat.setSubscriptionCount(Math.toIntExact(subscriptionMapper.selectCount(
                Wrappers.<UserSubscription>lambdaQuery().between(UserSubscription::getCreateTime, dayStart, dayEnd))));

        // 通知统计(CTR)
        stat.setNotificationSent(Math.toIntExact(messageMapper.selectCount(
                Wrappers.<Message>lambdaQuery().between(Message::getCreateTime, dayStart, dayEnd))));
        stat.setNotificationClicked(Math.toIntExact(messageMapper.selectCount(
                Wrappers.<Message>lambdaQuery()
                        .between(Message::getCreateTime, dayStart, dayEnd)
                        .eq(Message::getClicked, 1))));

        // 信誉贡献统计
        stat.setAcceptedCorrections(Math.toIntExact(correctionMapper.selectCount(
                Wrappers.<RobotParamCorrection>lambdaQuery()
                        .eq(RobotParamCorrection::getStatus, 1)  // 1=ACCEPTED
                        .between(RobotParamCorrection::getUpdateTime, dayStart, dayEnd))));

        // 默认值(需要行为日志系统支持才能精确统计)
        stat.setDau(0);
        stat.setRobotViews(0);
        stat.setSearches(0);
        stat.setFavorites(0);
        stat.setCompares(0);
        stat.setSelectionSearches(0);
        stat.setProcurements(0);
        stat.setBrandViews(0);
        stat.setCompanyViews(0);
        stat.setFollows(0);
        stat.setReturningUsers(0);
        stat.setContributionUsers(0);
        stat.setProcurementLeads(0);
        stat.setEnterpriseResponses(0);
        stat.setFollowConversions(0);
        stat.setCreateTime(LocalDateTime.now());

        return stat;
    }

    private void saveStat(GrowthDailyStat stat) {
        // 尝试插入，如果已存在则更新
        GrowthDailyStat existing = growthDailyStatMapper.selectOne(
                Wrappers.<GrowthDailyStat>lambdaQuery().eq(GrowthDailyStat::getStatDate, stat.getStatDate()));
        if (existing != null) {
            stat.setId(existing.getId());
            growthDailyStatMapper.updateById(stat);
        } else {
            try {
                growthDailyStatMapper.insert(stat);
            } catch (DuplicateKeyException e) {
                // 并发情况下可能重复插入，忽略
                log.info("增长日统计已存在: statDate={}", stat.getStatDate());
            }
        }
    }
}