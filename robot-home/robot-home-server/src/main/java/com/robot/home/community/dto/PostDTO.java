package com.robot.home.community.dto;

import lombok.Data;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * 发帖 / 编辑帖子参数
 */
@Data
public class PostDTO {

    private Long circleId;

    @Size(max = 100, message = "标题不能超过 100 字")
    private String title;

    @Size(max = 5000, message = "正文不能超过 5000 字")
    private String content;

    private List<String> images;

    private String videoUrl;

    private Long robotId;

    private Long brandId;

    @Size(max = 32, message = "话题不能超过 32 字")
    private String topic;
}
