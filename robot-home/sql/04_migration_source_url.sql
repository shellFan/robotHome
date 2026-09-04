-- ============================================================
-- 迁移：article 表增加 source_url 列
-- 用于记录文章的原始来源URL（采集来源页地址）
--
-- 注意：此脚本用于手动迁移已有数据库
-- Flyway 管理的数据库请使用 V2__add_article_source_url.sql
-- ============================================================
USE robot_home;

-- 安全添加列（已存在则跳过）
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article' AND COLUMN_NAME = 'source_url');

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `article` ADD COLUMN `source_url` VARCHAR(512) DEFAULT NULL COMMENT ''来源URL'' AFTER `source`',
    'SELECT ''source_url column already exists, skipping''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 安全添加索引（已存在则跳过）
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article' AND INDEX_NAME = 'idx_source_url');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `article` ADD INDEX `idx_source_url` (`source_url`(255))',
    'SELECT ''idx_source_url already exists, skipping''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;