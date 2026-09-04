package com.robot.home.robot.vo;

import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.entity.RobotImage;
import com.robot.home.robot.entity.RobotPrice;
import com.robot.home.robot.entity.RobotVideo;
import lombok.Data;

import java.util.List;

/**
 * 机器人详情视图对象
 */
@Data
public class RobotDetailVO {

    private Robot robot;
    private String brandName;
    private String brandLogo;
    private Long brandId;
    private String categoryName;
    private String parentCategoryName;
    private String companyName;
    private Long companyId;
    private List<RobotImage> images;
    private List<RobotVideo> videos;
    private List<RobotParamGroupVO> paramGroups;
    private List<RobotPrice> prices;
    private List<String> scenes;
    private List<String> devs;
    private List<String> ais;
    /** 相关资讯 */
    private List<RelatedArticleVO> articles;
    private Boolean favorited;
    private Long favoriteCount;
}
