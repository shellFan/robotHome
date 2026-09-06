-- ============================================================
-- V2: article 表增加 source_url 列（来源URL）
-- 已存在字段时安全处理（IF NOT EXISTS 语义通过条件判断实现）
-- ============================================================

-- 添加 source_url 列（如果不存在）
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article' AND COLUMN_NAME = 'source_url');

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `article` ADD COLUMN `source_url` VARCHAR(512) DEFAULT NULL COMMENT ''来源URL'' AFTER `source`',
    'SELECT ''source_url column already exists, skipping''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引（如果不存在）
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article' AND INDEX_NAME = 'idx_source_url');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `article` ADD INDEX `idx_source_url` (`source_url`(191))',
    'SELECT ''idx_source_url already exists, skipping''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;