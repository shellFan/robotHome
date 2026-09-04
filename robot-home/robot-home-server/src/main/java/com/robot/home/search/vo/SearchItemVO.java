package com.robot.home.search.vo;

import lombok.Data;

/**
 * 统一搜索结果项（便于前端按类型分组渲染）
 */
@Data
public class SearchItemVO {

    /** robot / brand / company / article / video / tutorial / post */
    private String type;
    private Long id;
    private String title;
    private String image;
    private String summary;
    private String url;
    /** 附加信息：价格、品牌名等 */
    private String extra;
}
