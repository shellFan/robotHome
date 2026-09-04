package com.robot.home.collector.adapter;

import com.robot.home.collector.fetcher.FetchResult;

import java.util.List;
import java.util.Map;

/**
 * 采集适配器接口
 * 不同类型的网站使用不同的适配器策略
 */
public interface CrawlerAdapter {

    /**
     * 获取适配器类型
     */
    String getType();

    /**
     * 判断是否支持该URL/数据源
     */
    boolean supports(String url, Map<String, String> config);

    /**
     * 发现种子URL
     * @param seedUrl 种子URL
     * @param config 数据源配置
     * @return 发现的URL列表
     */
    List<String> discoverUrls(String seedUrl, Map<String, String> config);

    /**
     * 解析页面内容
     * @param fetchResult 抓取结果
     * @param config 数据源配置
     * @return 解析后的结构化数据
     */
    ParsedData parse(FetchResult fetchResult, Map<String, String> config);

    /**
     * 提取页面中的链接
     * @param fetchResult 抓取结果
     * @param config 数据源配置
     * @return 页面中的链接列表
     */
    List<String> extractLinks(FetchResult fetchResult, Map<String, String> config);

    /**
     * 判断是否应该跟踪该链接
     */
    boolean shouldFollow(String url, int currentDepth, int maxDepth, Map<String, String> config);
}