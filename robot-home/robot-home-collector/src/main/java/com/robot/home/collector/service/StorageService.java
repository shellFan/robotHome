package com.robot.home.collector.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robot.home.collector.entity.*;
import com.robot.home.collector.mapper.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 存储服务
 * 负责采集数据的持久化存储
 * 支持数据库存储和API存储（推送至robot-home-server）
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    @Autowired
    private CrawlerArticleMapper articleMapper;

    @Autowired
    private CrawlerProductMapper productMapper;

    @Autowired
    private CrawlerMediaMapper mediaMapper;

    @Autowired
    private CrawlerPageMapper pageMapper;

    @Autowired
    private CrawlerUrlMapper urlMapper;

    @Autowired
    private CrawlerMatchRecordMapper matchRecordMapper;

    @Autowired
    private CrawlerProductChangeMapper productChangeMapper;

    // ==================== URL管理 ====================

    /**
     * 保存或更新URL记录
     */
    public CrawlerUrl saveUrl(CrawlerUrl crawlerUrl) {
        if (crawlerUrl.getId() == null) {
            urlMapper.insert(crawlerUrl);
        } else {
            urlMapper.updateById(crawlerUrl);
        }
        return crawlerUrl;
    }

    /**
     * 根据URL哈希查找
     */
    public CrawlerUrl findUrlByHash(String urlHash) {
        return urlMapper.selectOne(
                new LambdaQueryWrapper<CrawlerUrl>().eq(CrawlerUrl::getUrlHash, urlHash).last("LIMIT 1"));
    }

    // ==================== Page管理 ====================

    /**
     * 保存页面原始数据
     */
    @Transactional
    public CrawlerPage savePage(CrawlerPage page) {
        if (page.getId() == null) {
            pageMapper.insert(page);
        } else {
            pageMapper.updateById(page);
        }
        return page;
    }

    /**
     * 根据URL查找页面
     */
    public CrawlerPage findPageByUrl(String url) {
        return pageMapper.selectOne(
                new LambdaQueryWrapper<CrawlerPage>().eq(CrawlerPage::getUrl, url).last("LIMIT 1"));
    }

    // ==================== Article管理 ====================

    /**
     * 保存文章
     */
    @Transactional
    public CrawlerArticle saveArticle(CrawlerArticle article) {
        if (article.getId() == null) {
            articleMapper.insert(article);
        } else {
            articleMapper.updateById(article);
        }
        return article;
    }

    /**
     * 根据内容哈希查找文章
     */
    public CrawlerArticle findArticleByContentHash(String contentHash) {
        return articleMapper.selectOne(
                new LambdaQueryWrapper<CrawlerArticle>().eq(CrawlerArticle::getContentHash, contentHash).last("LIMIT 1"));
    }

    /**
     * 根据源URL查找文章
     */
    public CrawlerArticle findArticleBySourceUrl(String sourceUrl) {
        return articleMapper.selectOne(
                new LambdaQueryWrapper<CrawlerArticle>().eq(CrawlerArticle::getSourceUrl, sourceUrl).last("LIMIT 1"));
    }

    // ==================== Product管理 ====================

    /**
     * 保存产品
     */
    @Transactional
    public CrawlerProduct saveProduct(CrawlerProduct product) {
        if (product.getId() == null) {
            productMapper.insert(product);
        } else {
            productMapper.updateById(product);
        }
        return product;
    }

    /**
     * 根据源URL查找产品
     */
    public CrawlerProduct findProductBySourceUrl(String sourceUrl) {
        return productMapper.selectOne(
                new LambdaQueryWrapper<CrawlerProduct>().eq(CrawlerProduct::getSourceUrl, sourceUrl).last("LIMIT 1"));
    }

    /**
     * 根据品牌ID和产品名查找
     */
    public CrawlerProduct findProductByBrandAndName(Long brandId, String productName) {
        return productMapper.selectOne(
                new LambdaQueryWrapper<CrawlerProduct>()
                        .eq(CrawlerProduct::getBrandId, brandId)
                        .eq(CrawlerProduct::getProductName, productName)
                        .last("LIMIT 1"));
    }

    /**
     * 记录产品变更
     */
    @Transactional
    public void recordProductChange(CrawlerProductChange change) {
        productChangeMapper.insert(change);
    }

    // ==================== Media管理 ====================

    /**
     * 保存媒体
     */
    @Transactional
    public CrawlerMedia saveMedia(CrawlerMedia media) {
        if (media.getId() == null) {
            mediaMapper.insert(media);
        } else {
            mediaMapper.updateById(media);
        }
        return media;
    }

    /**
     * 根据URL哈希查找媒体
     */
    public CrawlerMedia findMediaByUrlHash(String urlHash) {
        return mediaMapper.selectOne(
                new LambdaQueryWrapper<CrawlerMedia>().eq(CrawlerMedia::getUrlHash, urlHash).last("LIMIT 1"));
    }

    // ==================== MatchRecord管理 ====================

    /**
     * 保存匹配记录
     */
    @Transactional
    public CrawlerMatchRecord saveMatchRecord(CrawlerMatchRecord record) {
        if (record.getId() == null) {
            matchRecordMapper.insert(record);
        } else {
            matchRecordMapper.updateById(record);
        }
        return record;
    }

    /**
     * 查找实体的匹配记录
     */
    public List<CrawlerMatchRecord> findMatchRecords(String entityType, Long entityId) {
        return matchRecordMapper.selectList(
                new LambdaQueryWrapper<CrawlerMatchRecord>()
                        .eq(CrawlerMatchRecord::getBizType, entityType)
                        .eq(CrawlerMatchRecord::getBizId, entityId)
                        .orderByDesc(CrawlerMatchRecord::getCreateTime));
    }

    // ==================== 综合存储 ====================

    /**
     * 存储解析后的文章数据
     * 包含：URL更新、Page保存、Article保存、MatchRecord保存
     */
    @Transactional
    public CrawlerArticle storeArticle(CrawlerUrl crawlerUrl, CrawlerPage page, CrawlerArticle article,
                                        CrawlerMatchRecord matchRecord) {
        // 1. 更新URL状态
        if (crawlerUrl != null) {
            crawlerUrl.setUrlStatus("PARSED");
            saveUrl(crawlerUrl);
        }

        // 2. 保存页面
        if (page != null) {
            savePage(page);
        }

        // 3. 保存文章
        saveArticle(article);

        // 4. 保存匹配记录
        if (matchRecord != null) {
            matchRecord.setBizType("article");
            matchRecord.setBizId(article.getId());
            saveMatchRecord(matchRecord);
        }

        log.info("Stored article: id={}, title={}, brand={}", article.getId(), article.getTitle(), article.getBrandId());
        return article;
    }

    /**
     * 存储解析后的产品数据
     */
    @Transactional
    public CrawlerProduct storeProduct(CrawlerUrl crawlerUrl, CrawlerPage page, CrawlerProduct product,
                                        CrawlerMatchRecord matchRecord) {
        if (crawlerUrl != null) {
            crawlerUrl.setUrlStatus("PARSED");
            saveUrl(crawlerUrl);
        }
        if (page != null) {
            savePage(page);
        }
        saveProduct(product);
        if (matchRecord != null) {
            matchRecord.setBizType("product");
            matchRecord.setBizId(product.getId());
            saveMatchRecord(matchRecord);
        }

        log.info("Stored product: id={}, name={}, brand={}", product.getId(), product.getProductName(), product.getBrandId());
        return product;
    }
}