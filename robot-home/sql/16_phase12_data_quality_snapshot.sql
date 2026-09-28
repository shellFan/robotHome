-- Phase12: 数据质量快照表 - 支持BEFORE/AFTER持久化对比
-- MySQL 5.6兼容: 无CTE/窗口函数/JSON列
-- 幂等: IF NOT EXISTS

CREATE TABLE IF NOT EXISTS `data_quality_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `label` VARCHAR(50) NOT NULL COMMENT '快照标签: BEFORE/AFTER/SNAPSHOT',
  `snapshot_time` DATETIME NOT NULL COMMENT '快照时间',
  `environment` VARCHAR(50) DEFAULT NULL COMMENT '环境: PRODUCTION/STAGING/TEST',
  -- Robot Coverage
  `robot_total` BIGINT DEFAULT 0,
  `robot_cover_image_rate` DOUBLE DEFAULT 0,
  `robot_gallery_rate` DOUBLE DEFAULT 0,
  `robot_param_rate` DOUBLE DEFAULT 0,
  `robot_source_url_rate` DOUBLE DEFAULT 0,
  `robot_brand_rate` DOUBLE DEFAULT 0,
  `robot_category_rate` DOUBLE DEFAULT 0,
  -- Source Health
  `source_total` BIGINT DEFAULT 0,
  `source_healthy` BIGINT DEFAULT 0,
  `source_degraded` BIGINT DEFAULT 0,
  `source_failed` BIGINT DEFAULT 0,
  `source_unknown` BIGINT DEFAULT 0,
  -- Freshness
  `robot_fresh` BIGINT DEFAULT 0,
  `robot_aging` BIGINT DEFAULT 0,
  `robot_stale` BIGINT DEFAULT 0,
  -- Brand & Article
  `brand_total` BIGINT DEFAULT 0,
  `brand_website_rate` DOUBLE DEFAULT 0,
  `article_total` BIGINT DEFAULT 0,
  `article_cover_image_rate` DOUBLE DEFAULT 0,
  `article_source_url_rate` DOUBLE DEFAULT 0,
  -- Crawler Pipeline
  `crawler_product_total` BIGINT DEFAULT 0,
  `crawler_product_pending_review` BIGINT DEFAULT 0,
  `crawler_product_published` BIGINT DEFAULT 0,
  `crawler_article_total` BIGINT DEFAULT 0,
  `crawler_article_pending_review` BIGINT DEFAULT 0,
  `crawler_article_published` BIGINT DEFAULT 0,
  -- Overall Score
  `overall_score` DOUBLE DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_label` (`label`),
  INDEX `idx_snapshot_time` (`snapshot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据质量快照表';