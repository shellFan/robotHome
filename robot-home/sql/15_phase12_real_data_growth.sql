-- ============================================================
-- Phase12: Real Data & Content Growth 数据库迁移
-- 从Phase10/11 DB执行此脚本必须成功
-- 字符集：utf8mb4 / 排序：utf8mb4_general_ci
-- 兼容 MySQL 5.6：无窗口函数/CTE/JSON列/utf8mb4_0900
-- 幂等性：所有ALTER TABLE ADD COLUMN/ADD INDEX均带IF NOT EXISTS保护
-- ============================================================

USE robot_home;

-- ============================================================
-- 幂等性辅助：添加列/索引的存储过程（若Phase10已创建则跳过）
-- ============================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS `p_add_column`$$
CREATE PROCEDURE `p_add_column`(
  IN p_table VARCHAR(64),
  IN p_column VARCHAR(64),
  IN p_definition VARCHAR(500)
)
BEGIN
  DECLARE col_exists INT DEFAULT 0;
  SELECT COUNT(*) INTO col_exists
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column;
  IF col_exists = 0 THEN
    SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

DROP PROCEDURE IF EXISTS `p_add_index`$$
CREATE PROCEDURE `p_add_index`(
  IN p_table VARCHAR(64),
  IN p_index VARCHAR(64),
  IN p_columns VARCHAR(500)
)
BEGIN
  DECLARE idx_exists INT DEFAULT 0;
  SELECT COUNT(*) INTO idx_exists
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index;
  IF idx_exists = 0 THEN
    SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_columns, ')');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

DELIMITER ;

-- ============================================================
-- P0-1: crawler_source 健康度持久化字段
-- Entity有9个字段在DB中无对应列，需补齐
-- ============================================================

CALL `p_add_column`('crawler_source', 'consecutive_failures', 'INT DEFAULT 0 COMMENT ''连续失败次数'' AFTER `health_status`');
CALL `p_add_column`('crawler_source', 'avg_latency_ms', 'INT DEFAULT 0 COMMENT ''平均延迟(毫秒)'' AFTER `consecutive_failures`');
CALL `p_add_column`('crawler_source', 'last_success_time', 'DATETIME DEFAULT NULL COMMENT ''最后成功时间'' AFTER `avg_latency_ms`');
CALL `p_add_column`('crawler_source', 'last_fail_time', 'DATETIME DEFAULT NULL COMMENT ''最后失败时间'' AFTER `last_success_time`');
CALL `p_add_column`('crawler_source', 'last_fail_reason', 'VARCHAR(512) DEFAULT NULL COMMENT ''最后失败原因'' AFTER `last_fail_time`');
CALL `p_add_column`('crawler_source', 'total_crawls', 'INT DEFAULT 0 COMMENT ''累计采集次数'' AFTER `last_fail_reason`');
CALL `p_add_column`('crawler_source', 'total_success_crawls', 'INT DEFAULT 0 COMMENT ''累计成功采集次数'' AFTER `total_crawls`');
CALL `p_add_column`('crawler_source', 'robots_checked', 'TINYINT DEFAULT 0 COMMENT ''是否已检查robots.txt: 0否 1是'' AFTER `total_success_crawls`');
CALL `p_add_column`('crawler_source', 'robots_allowed', 'TINYINT DEFAULT NULL COMMENT ''robots.txt是否允许抓取: 0否 1是'' AFTER `robots_checked`');
CALL `p_add_column`('crawler_source', 'robots_checked_time', 'DATETIME DEFAULT NULL COMMENT ''robots.txt检查时间'' AFTER `robots_allowed`');

-- 健康度索引
CALL `p_add_index`('crawler_source', 'idx_health_status', '`health_status`');

-- ============================================================
-- P0-2: crawler_source 信任等级
-- Source需区分OFFICIAL/TRUSTED/NORMAL，TrustService按权重计算
-- ============================================================

CALL `p_add_column`('crawler_source', 'trust_level', 'VARCHAR(16) DEFAULT ''NORMAL'' COMMENT ''信任等级: OFFICIAL/TRUSTED/NORMAL'' AFTER `region`');
CALL `p_add_index`('crawler_source', 'idx_trust_level', '`trust_level`');

-- 更新已有Source的trust_level：品牌官网=OFFICIAL，RSS媒体=TRUSTED，其他=NORMAL
UPDATE `crawler_source` SET `trust_level` = 'OFFICIAL' WHERE `source_type` = 'WEBSITE' AND `brand_id` IS NOT NULL;
UPDATE `crawler_source` SET `trust_level` = 'TRUSTED' WHERE `source_type` = 'RSS';

-- ============================================================
-- P0-1: robot Freshness 字段
-- 支持FRESH(≤30d)/AGING(31-90d)/STALE(>90d)新鲜度计算
-- ============================================================

CALL `p_add_column`('robot', 'freshness', 'VARCHAR(16) DEFAULT ''STALE'' COMMENT ''新鲜度: FRESH/AGING/STALE'' AFTER `trust_score`');
CALL `p_add_column`('robot', 'last_content_update_time', 'DATETIME DEFAULT NULL COMMENT ''最后内容更新时间(用于Freshness计算)'' AFTER `freshness`');
CALL `p_add_index`('robot', 'idx_freshness', '`freshness`');

-- ============================================================
-- P0-5: DataQuality 统计视图支持
-- 为覆盖率统计添加必要索引
-- ============================================================

CALL `p_add_index`('robot', 'idx_data_source', '`data_source`');
CALL `p_add_index`('robot', 'idx_cover_image', '`cover_image`(50)');

-- ============================================================
-- 清理存储过程
-- ============================================================

DROP PROCEDURE IF EXISTS `p_add_column`;
DROP PROCEDURE IF EXISTS `p_add_index`;