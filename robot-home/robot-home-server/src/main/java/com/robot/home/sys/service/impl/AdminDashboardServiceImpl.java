package com.robot.home.sys.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.article.entity.Article;
import com.robot.home.article.mapper.ArticleMapper;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.comment.entity.Comment;
import com.robot.home.comment.mapper.CommentMapper;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.community.entity.CommunityPost;
import com.robot.home.community.mapper.CommunityPostMapper;
import com.robot.home.inquiry.entity.Inquiry;
import com.robot.home.inquiry.mapper.InquiryMapper;
import com.robot.home.feedback.entity.UserFeedback;
import com.robot.home.feedback.mapper.UserFeedbackMapper;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.sys.service.AdminDashboardService;
import com.robot.home.sys.vo.DashboardStatsVO;
import com.robot.home.sys.vo.DashboardTrendVO;
import com.robot.home.tutorial.entity.Tutorial;
import com.robot.home.tutorial.mapper.TutorialMapper;
import com.robot.home.user.entity.User;
import com.robot.home.user.mapper.UserMapper;
import com.robot.home.video.entity.Video;
import com.robot.home.video.mapper.VideoMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 后台 Dashboard 统计实现
 */
@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Resource
    private UserMapper userMapper;
    @Resource
    private RobotMapper robotMapper;
    @Resource
    private BrandMapper brandMapper;
    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private VideoMapper videoMapper;
    @Resource
    private TutorialMapper tutorialMapper;
    @Resource
    private CommunityPostMapper postMapper;
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private InquiryMapper inquiryMapper;
    @Resource
    private UserFeedbackMapper userFeedbackMapper;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public DashboardStatsVO stats() {
        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setUserCount(userMapper.selectCount(Wrappers.<User>lambdaQuery()));
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        vo.setTodayNewUserCount(userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .ge(User::getCreateTime, todayStart)));
        vo.setRobotCount(robotMapper.selectCount(Wrappers.<Robot>lambdaQuery()));
        vo.setBrandCount(brandMapper.selectCount(Wrappers.<Brand>lambdaQuery()));
        vo.setCompanyCount(companyMapper.selectCount(Wrappers.<Company>lambdaQuery()));
        vo.setArticleCount(articleMapper.selectCount(Wrappers.<Article>lambdaQuery()));
        vo.setVideoCount(videoMapper.selectCount(Wrappers.<Video>lambdaQuery()));
        vo.setTutorialCount(tutorialMapper.selectCount(Wrappers.<Tutorial>lambdaQuery()));
        vo.setPostCount(postMapper.selectCount(Wrappers.<CommunityPost>lambdaQuery()));
        vo.setInquiryCount(inquiryMapper.selectCount(Wrappers.<Inquiry>lambdaQuery()));
        vo.setPendingInquiryCount(inquiryMapper.selectCount(Wrappers.<Inquiry>lambdaQuery()
                .eq(Inquiry::getStatus, 1)));
        vo.setFeedbackCount(userFeedbackMapper.selectCount(Wrappers.<UserFeedback>lambdaQuery()));
        vo.setPendingFeedbackCount(userFeedbackMapper.selectCount(Wrappers.<UserFeedback>lambdaQuery()
                .eq(UserFeedback::getStatus, 0)));
        vo.setCommentCount(commentMapper.selectCount(Wrappers.<Comment>lambdaQuery()));
        vo.setTodayPv(pvOf(LocalDate.now()));
        vo.setTodayUv(uvOf(LocalDate.now()));
        return vo;
    }

    @Override
    public List<DashboardTrendVO> trend(int days) {
        int n = Math.max(1, Math.min(days, 30));
        List<DashboardTrendVO> list = new ArrayList<>();
        for (int i = n - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            DashboardTrendVO vo = new DashboardTrendVO();
            vo.setDate(date.toString());
            vo.setPv(pvOf(date));
            vo.setUv(uvOf(date));
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            vo.setNewUsers(userMapper.selectCount(Wrappers.<User>lambdaQuery()
                    .ge(User::getCreateTime, start).lt(User::getCreateTime, end)));
            vo.setInquiries(inquiryMapper.selectCount(Wrappers.<Inquiry>lambdaQuery()
                    .ge(Inquiry::getCreateTime, start).lt(Inquiry::getCreateTime, end)));
            list.add(vo);
        }
        return list;
    }

    private long pvOf(LocalDate date) {
        String v = redisUtils.get("robot:stat:pv:" + date.format(DAY));
        return v == null ? 0L : Long.parseLong(v);
    }

    private long uvOf(LocalDate date) {
        Long size = redisUtils.sSize("robot:stat:uv:" + date.format(DAY));
        return size == null ? 0L : size;
    }
}
