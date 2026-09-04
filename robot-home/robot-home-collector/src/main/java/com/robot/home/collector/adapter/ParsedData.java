package com.robot.home.collector.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析后的结构化数据
 */
public class ParsedData {

    /** 数据类型：article, product, company, brand */
    private String type;

    /** 标题 */
    private String title;

    /** 正文纯文本 */
    private String content;

    /** 正文HTML */
    private String contentHtml;

    /** 摘要 */
    private String summary;

    /** 作者 */
    private String author;

    /** 发布时间 */
    private String publishDate;

    /** 来源URL */
    private String sourceUrl;

    /** 图片URL列表 */
    private List<String> images = new ArrayList<>();

    /** 标签/关键词 */
    private List<String> tags = new ArrayList<>();

    /** 动态参数（产品规格等） */
    private Map<String, String> params = new HashMap<>();

    /** 额外元数据 */
    private Map<String, String> metadata = new HashMap<>();

    /** 原始HTML */
    private String rawHtml;

    /** 是否解析成功 */
    private boolean success = true;

    /** 错误信息 */
    private String error;

    public static ParsedData fail(String error) {
        ParsedData data = new ParsedData();
        data.setSuccess(false);
        data.setError(error);
        return data;
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getPublishDate() { return publishDate; }
    public void setPublishDate(String publishDate) { this.publishDate = publishDate; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public Map<String, String> getParams() { return params; }
    public void setParams(Map<String, String> params) { this.params = params; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    public String getRawHtml() { return rawHtml; }
    public void setRawHtml(String rawHtml) { this.rawHtml = rawHtml; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public void addImage(String url) { images.add(url); }
    public void addTag(String tag) { tags.add(tag); }
    public void addParam(String key, String value) { params.put(key, value); }
    public void addMetadata(String key, String value) { metadata.put(key, value); }
}