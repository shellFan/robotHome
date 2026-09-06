-- ============================================================
-- V4: 添加 next_retry_time 列，支持指数退避重试策略
-- 已有数据库：条件添加列（若列不存在则添加）
-- 全新数据库：V1 基线已包含此列，此迁移为空操作
-- ============================================================

-- crawler_article: 添加 next_retry_time
SET @exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crawler_article' AND COLUMN_NAME = 'next_retry_time');
SET @sql = IF(@exists = 0,
    'ALTER TABLE `crawler_article` ADD COLUMN `next_retry_time` DATETIME DEFAULT NULL COMMENT ''下次重试时间（指数退避）''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- crawler_product: 添加 next_retry_time
SET @exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crawler_product' AND COLUMN_NAME = 'next_retry_time');
SET @sql = IF(@exists = 0,
    'ALTER TABLE `crawler_product` ADD COLUMN `next_retry_time` DATETIME DEFAULT NULL COMMENT ''下次重试时间（指数退避）''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;