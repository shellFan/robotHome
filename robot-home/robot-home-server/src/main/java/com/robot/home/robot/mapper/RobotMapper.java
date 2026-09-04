package com.robot.home.robot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.robot.home.robot.dto.RobotQuery;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.vo.RobotListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 机器人 Mapper：复杂筛选分页走 XML，简单操作走 MyBatis-Plus
 */
@Mapper
public interface RobotMapper extends BaseMapper<Robot> {

    /**
     * 机器人库分页筛选（含品牌名 / 分类名 / 标签多维过滤）
     */
    IPage<RobotListVO> selectRobotPage(IPage<?> page, @Param("q") RobotQuery query);

    /**
     * 按 id 列表查询（顺序由调用方在 Java 中还原，避免依赖数据库专有函数）
     */
    List<RobotListVO> selectListByIds(@Param("ids") List<Long> ids);
}
