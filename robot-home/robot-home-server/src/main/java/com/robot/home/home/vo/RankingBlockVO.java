package com.robot.home.home.vo;

import com.robot.home.robot.vo.RobotListVO;
import lombok.Data;

import java.util.List;

/**
 * 首页榜单区块
 */
@Data
public class RankingBlockVO {

    private String code;
    private String name;
    private List<RobotListVO> robots;
}
