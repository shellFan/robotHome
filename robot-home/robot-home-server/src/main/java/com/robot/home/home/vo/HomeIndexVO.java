package com.robot.home.home.vo;

import com.robot.home.article.vo.ArticleListVO;
import com.robot.home.banner.entity.Banner;
import com.robot.home.brand.vo.BrandListVO;
import com.robot.home.community.vo.PostVO;
import com.robot.home.company.vo.CompanyListVO;
import com.robot.home.robot.vo.CategoryNodeVO;
import com.robot.home.robot.vo.RobotListVO;
import com.robot.home.video.vo.VideoListVO;
import lombok.Data;

import java.util.List;

/**
 * 首页聚合数据
 */
@Data
public class HomeIndexVO {

    private List<Banner> banners;
    /** 快捷导航：一级分类 */
    private List<CategoryNodeVO> quickNav;
    private List<RobotListVO> hotRobots;
    private List<RobotListVO> newRobots;
    private List<BrandListVO> hotBrands;
    private List<ArticleListVO> articles;
    private List<VideoListVO> videos;
    private List<PostVO> hotPosts;
    private List<CompanyListVO> companies;
    /** 榜单卡片（热门 / 人形 / 机器狗 / 工业） */
    private List<RankingBlockVO> rankings;
}
