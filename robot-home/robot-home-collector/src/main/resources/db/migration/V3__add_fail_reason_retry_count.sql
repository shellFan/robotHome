-- ============================================================
-- V3: 采集文章/产品表增加失败原因和重试次数字段
-- 使用 PREPARE/EXECUTE 条件判断，兼容 V1 基线已包含这些列的情况
-- ============================================================

-- crawler_article 增加失败原因
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crawler_article' AND COLUMN_NAME = 'fail_reason');

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `crawler_article` ADD COLUMN `fail_reason` VARCHAR(1024) DEFAULT NULL COMMENT ''失败原因'' AFTER `synced`',
    'SELECT ''crawler_article.fail_reason already exists, skipping''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- crawler_article 增加重试次数
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crawler_article' AND COLUMN_NAME = 'retry_count');

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `crawler_article` ADD COLUMN `retry_count` INT DEFAULT 0 COMMENT ''重试次数'' AFTER `fail_reason`',
    'SELECT ''crawler_article.retry_count already exists, skipping''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- crawler_product 增加失败原因
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crawler_product' AND COLUMN_NAME = 'fail_reason');

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `crawler_product` ADD COLUMN `fail_reason` VARCHAR(1024) DEFAULT NULL COMMENT ''失败原因'' AFTER `synced`',
    'SELECT ''crawler_product.fail_reason already exists, skipping''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- crawler_product 增加重试次数
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crawler_product' AND COLUMN_NAME = 'retry_count');

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `crawler_product` ADD COLUMN `retry_count` INT DEFAULT 0 COMMENT ''重试次数'' AFTER `fail_reason`',
    'SELECT ''crawler_product.retry_count already exists, skipping''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;