package com.robot.home.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.collector.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 采集规则
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crawler_rule")
public class CrawlerRule extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 数据源ID(NULL=全局规则) */
    private Long sourceId;

    /** 规则类型: SELECTOR/REGEX/KEYWORD/PARAM_MAP/CATEGORY */
    private String ruleType;

    /** 规则名称 */
    private String ruleName;

    /** 规则键(如title_selector) */
    private String ruleKey;

    /** 规则值(如CSS选择器/正则/JSON) */
    private String ruleValue;

    /** 优先级(越大越优先) */
    private Integer priority;

    /** 是否启用: 0禁用 1启用 */
    private Integer enabled;
}