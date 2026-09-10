-- ============================================================
-- 机器人之家 Phase 5 迁移：采集源健康度 + 页面分类器增强
-- 兼容 MySQL 5.6
-- ============================================================
USE robot_home;

-- ---------------------------
-- 采集源健康度字段
-- ---------------------------
ALTER TABLE `crawler_source` ADD COLUMN `health_status` VARCHAR(16) DEFAULT 'UNKNOWN' COMMENT '健康状态: HEALTHY/DEGRADED/FAILED/DISABLED/UNKNOWN' AFTER `total_errors`;
ALTER TABLE `crawler_source` ADD COLUMN `consecutive_failures` INT DEFAULT 0 COMMENT '连续失败次数' AFTER `health_status`;
ALTER TABLE `crawler_source` ADD COLUMN `avg_latency_ms` INT DEFAULT NULL COMMENT '平均延迟(毫秒)' AFTER `consecutive_failures`;
ALTER TABLE `crawler_source` ADD COLUMN `last_success_time` DATETIME DEFAULT NULL COMMENT '最后成功时间' AFTER `avg_latency_ms`;
ALTER TABLE `crawler_source` ADD COLUMN `last_fail_time` DATETIME DEFAULT NULL COMMENT '最后失败时间' AFTER `last_success_time`;
ALTER TABLE `crawler_source` ADD COLUMN `last_fail_reason` VARCHAR(512) DEFAULT NULL COMMENT '最后失败原因' AFTER `last_fail_time`;
ALTER TABLE `crawler_source` ADD COLUMN `total_crawls` INT DEFAULT 0 COMMENT '累计采集次数' AFTER `last_fail_reason`;
ALTER TABLE `crawler_source` ADD COLUMN `total_success_crawls` INT DEFAULT 0 COMMENT '累计成功采集次数' AFTER `total_crawls`;

-- robots.txt 检查字段
ALTER TABLE `crawler_source` ADD COLUMN `robots_checked` TINYINT DEFAULT 0 COMMENT '是否已检查robots.txt' AFTER `total_success_crawls`;
ALTER TABLE `crawler_source` ADD COLUMN `robots_allowed` TINYINT DEFAULT 1 COMMENT 'robots.txt是否允许抓取' AFTER `robots_checked`;
ALTER TABLE `crawler_source` ADD COLUMN `robots_checked_time` DATETIME DEFAULT NULL COMMENT 'robots.txt检查时间' AFTER `robots_allowed`;

-- 采集源分类增强
ALTER TABLE `crawler_source` ADD COLUMN `rss_url` VARCHAR(512) DEFAULT NULL COMMENT 'RSS/Atom Feed URL' AFTER `sitemap_url`;
ALTER TABLE `crawler_source` ADD COLUMN `language` VARCHAR(8) DEFAULT 'zh' COMMENT '站点语言: zh/en/ja/ko等' AFTER `robots_checked_time`;
ALTER TABLE `crawler_source` ADD COLUMN `region` VARCHAR(16) DEFAULT 'CN' COMMENT '站点地区: CN/US/JP/KR/EU等' AFTER `language`;
ALTER TABLE `crawler_source` ADD COLUMN `priority` INT DEFAULT 5 COMMENT '采集优先级(1-10, 10最高)' AFTER `region`;
ALTER TABLE `crawler_source` ADD COLUMN `tags` VARCHAR(512) DEFAULT NULL COMMENT '标签(逗号分隔)' AFTER `priority`;

-- 健康状态索引
ALTER TABLE `crawler_source` ADD INDEX `idx_health` (`health_status`);
ALTER TABLE `crawler_source` ADD INDEX `idx_priority` (`priority`);

-- ---------------------------
-- 采集错误表增强
-- ---------------------------
ALTER TABLE `crawler_error` ADD COLUMN `http_status` INT DEFAULT NULL COMMENT 'HTTP状态码' AFTER `error_type`;
ALTER TABLE `crawler_error` ADD COLUMN `response_time_ms` INT DEFAULT NULL COMMENT '响应时间(毫秒)' AFTER `http_status`;
ALTER TABLE `crawler_error` ADD COLUMN `error_category` VARCHAR(32) DEFAULT NULL COMMENT '错误分类: NETWORK/DNS/TIMEOUT/HTTP_4XX/HTTP_5XX/ROBOTS/PARSE/DUPLICATE/OTHER' AFTER `response_time_ms`;

-- 错误分类索引
ALTER TABLE `crawler_error` ADD INDEX `idx_error_category` (`error_category`);