-- ============================================================
-- Phase13: Growth & Enterprise Ecosystem 数据库迁移
-- 从Phase12 DB执行此脚本必须成功
-- 字符集：utf8mb4 / 排序：utf8mb4_general_ci
-- 兼容 MySQL 5.6：无窗口函数/CTE/JSON列/utf8mb4_0900
-- 幂等性：所有ALTER TABLE ADD COLUMN/ADD INDEX均带保护
-- ============================================================

USE robot_home;

-- ============================================================
-- 幂等性辅助：添加列/索引的存储过程（若已创建则跳过）
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

DROP PROCEDURE IF EXISTS `p_add_unique_index`$$
CREATE PROCEDURE `p_add_unique_index`(
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
    SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD UNIQUE INDEX `', p_index, '` (', p_columns, ')');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

DROP PROCEDURE IF EXISTS `p_drop_index`$$
CREATE PROCEDURE `p_drop_index`(
  IN p_table VARCHAR(64),
  IN p_index VARCHAR(64)
)
BEGIN
  DECLARE idx_exists INT DEFAULT 0;
  SELECT COUNT(*) INTO idx_exists
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index;
  IF idx_exists > 0 THEN
    SET @sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$

DELIMITER ;

-- ============================================================
-- P0: SEO Slug 列 — 为公开落地页提供稳定唯一URL
-- slug生成规则: 应用层拼音/英文/编号，确保唯一
-- ============================================================

CALL `p_add_column`('robot', 'slug', 'VARCHAR(128) DEFAULT NULL COMMENT ''SEO slug'' AFTER `name`');
CALL `p_add_index`('robot', 'idx_slug', '`slug`');

CALL `p_add_column`('brand', 'slug', 'VARCHAR(128) DEFAULT NULL COMMENT ''SEO slug'' AFTER `name`');
CALL `p_add_index`('brand', 'idx_slug', '`slug`');

CALL `p_add_column`('company', 'slug', 'VARCHAR(128) DEFAULT NULL COMMENT ''SEO slug'' AFTER `name`');
CALL `p_add_index`('company', 'idx_slug', '`slug`');

CALL `p_add_column`('article', 'slug', 'VARCHAR(128) DEFAULT NULL COMMENT ''SEO slug'' AFTER `title`');
CALL `p_add_index`('article', 'idx_slug', '`slug`');

CALL `p_add_column`('robot_category', 'slug', 'VARCHAR(128) DEFAULT NULL COMMENT ''SEO slug'' AFTER `name`');
CALL `p_add_index`('robot_category', 'idx_slug', '`slug`');

-- ============================================================
-- P1: 企业认证状态列 — UNCLAIMED/PENDING/VERIFIED/REJECTED/SUSPENDED
-- ============================================================

CALL `p_add_column`('company', 'verification_status', 'VARCHAR(20) DEFAULT ''UNCLAIMED'' COMMENT ''认证状态: UNCLAIMED/PENDING/VERIFIED/REJECTED/SUSPENDED'' AFTER `status`');
CALL `p_add_index`('company', 'idx_verification_status', '`verification_status`');

-- ============================================================
-- P1: 企业认领申请表
-- ============================================================

CREATE TABLE IF NOT EXISTS `company_claim` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL COMMENT '企业ID',
  `applicant_user_id` BIGINT NOT NULL COMMENT '申请人用户ID',
  `pending_company_id` BIGINT DEFAULT NULL COMMENT '仅待审时占用企业槽位',
  `pending_applicant_user_id` BIGINT DEFAULT NULL COMMENT '仅待审时占用申请人槽位',
  `applicant_name` VARCHAR(64) DEFAULT NULL COMMENT '申请人姓名',
  `applicant_phone` VARCHAR(32) DEFAULT NULL COMMENT '申请人联系电话',
  `applicant_email` VARCHAR(128) DEFAULT NULL COMMENT '申请人邮箱',
  `applicant_position` VARCHAR(64) DEFAULT NULL COMMENT '申请人在企业中的职位',
  `claim_reason` VARCHAR(512) DEFAULT NULL COMMENT '认领理由',
  `proof_materials` TEXT COMMENT '证明材料URL列表(JSON数组,私有存储)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED/CANCELLED',
  `reviewer_user_id` BIGINT DEFAULT NULL COMMENT '审核人用户ID',
  `reviewer_name` VARCHAR(64) DEFAULT NULL COMMENT '审核人姓名',
  `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `review_comment` VARCHAR(512) DEFAULT NULL COMMENT '审核意见',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_company_active_claim` (`pending_company_id`),
  UNIQUE INDEX `uk_applicant_active_claim` (`pending_applicant_user_id`),
  INDEX `idx_applicant` (`applicant_user_id`),
  INDEX `idx_company` (`company_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业认领申请';

-- 兼容已经建立过旧版 Phase13 草稿表的环境，并迁移其待审记录。
CALL `p_add_column`('company_claim', 'pending_company_id', 'BIGINT DEFAULT NULL COMMENT ''仅待审时占用企业槽位'' AFTER `applicant_user_id`');
CALL `p_add_column`('company_claim', 'pending_applicant_user_id', 'BIGINT DEFAULT NULL COMMENT ''仅待审时占用申请人槽位'' AFTER `pending_company_id`');
CALL `p_drop_index`('company_claim', 'uk_company_pending');
-- Earlier drafts could allow one user to hold several pending requests. Keep the oldest request
-- and retain the others as cancelled history before adding the applicant active-slot constraint.
DROP TEMPORARY TABLE IF EXISTS `phase13_claim_pending_keep`;
CREATE TEMPORARY TABLE `phase13_claim_pending_keep` (`id` BIGINT NOT NULL PRIMARY KEY);
INSERT INTO `phase13_claim_pending_keep` (`id`)
SELECT MIN(`id`) FROM `company_claim`
WHERE `status` = 'PENDING' AND `deleted` = 0
GROUP BY `applicant_user_id`;
UPDATE `company_claim` c
LEFT JOIN `phase13_claim_pending_keep` k ON c.`id` = k.`id`
SET c.`status` = 'CANCELLED',
    c.`review_comment` = COALESCE(c.`review_comment`, '自动关闭：历史数据中同一用户存在多个待审申请'),
    c.`review_time` = COALESCE(c.`review_time`, CURRENT_TIMESTAMP),
    c.`pending_company_id` = NULL,
    c.`pending_applicant_user_id` = NULL
WHERE c.`status` = 'PENDING' AND c.`deleted` = 0 AND k.`id` IS NULL;
DROP TEMPORARY TABLE `phase13_claim_pending_keep`;
UPDATE `company_claim`
SET `pending_company_id` = `company_id`, `pending_applicant_user_id` = `applicant_user_id`
WHERE `status` = 'PENDING' AND `deleted` = 0
  AND `pending_company_id` IS NULL AND `pending_applicant_user_id` IS NULL;
CALL `p_add_unique_index`('company_claim', 'uk_company_active_claim', '`pending_company_id`');
CALL `p_add_unique_index`('company_claim', 'uk_applicant_active_claim', '`pending_applicant_user_id`');

-- ============================================================
-- P2: 企业资料变更申请表 — 高风险字段修改走审核
-- ============================================================

CREATE TABLE IF NOT EXISTS `company_profile_change` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL COMMENT '企业ID',
  `submitter_user_id` BIGINT NOT NULL COMMENT '提交人用户ID',
  `submitter_name` VARCHAR(64) DEFAULT NULL COMMENT '提交人姓名',
  `change_type` VARCHAR(32) NOT NULL COMMENT '变更类型: PROFILE/LOGO/WEBSITE/CONTACT/INTRO',
  `field_name` VARCHAR(64) NOT NULL COMMENT '字段名',
  `field_label` VARCHAR(64) DEFAULT NULL COMMENT '字段显示名称',
  `old_value` TEXT COMMENT '旧值',
  `new_value` TEXT COMMENT '新值',
  `change_reason` VARCHAR(512) DEFAULT NULL COMMENT '变更理由',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
  `reviewer_user_id` BIGINT DEFAULT NULL COMMENT '审核人用户ID',
  `reviewer_name` VARCHAR(64) DEFAULT NULL COMMENT '审核人姓名',
  `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `review_comment` VARCHAR(512) DEFAULT NULL COMMENT '审核意见',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  INDEX `idx_company` (`company_id`),
  INDEX `idx_submitter` (`submitter_user_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业资料变更申请';

CALL `p_add_column`('company_profile_change', 'field_label', 'VARCHAR(64) DEFAULT NULL COMMENT ''字段显示名称'' AFTER `field_name`');
CALL `p_add_column`('company_profile_change', 'change_reason', 'VARCHAR(512) DEFAULT NULL COMMENT ''变更理由'' AFTER `new_value`');
CALL `p_add_column`('company_profile_change', 'reviewer_name', 'VARCHAR(64) DEFAULT NULL COMMENT ''审核人姓名'' AFTER `reviewer_user_id`');

-- ============================================================
-- P4: SEO页面访问事件表 — 增长漏斗数据源
-- ============================================================

CREATE TABLE IF NOT EXISTS `seo_page_view` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `page_type` VARCHAR(32) NOT NULL COMMENT '页面类型: ROBOT/BRAND/COMPANY/CATEGORY/ARTICLE/RANKING',
  `page_id` BIGINT DEFAULT NULL COMMENT '页面业务ID',
  `entity_id` BIGINT DEFAULT NULL COMMENT '关联实体ID',
  `page_slug` VARCHAR(128) DEFAULT NULL COMMENT '页面slug',
  `page_path` VARCHAR(512) DEFAULT NULL COMMENT '页面路径',
  `visitor_id` VARCHAR(64) DEFAULT NULL COMMENT '访客唯一标识(匿名cookie或userId)',
  `user_id` BIGINT DEFAULT NULL COMMENT '登录用户ID(未登录为null)',
  `referer` VARCHAR(512) DEFAULT NULL COMMENT '来源页',
  `referer_source` VARCHAR(32) DEFAULT NULL COMMENT '来源类型',
  `referer_url` VARCHAR(512) DEFAULT NULL COMMENT '来源URL',
  `user_agent` VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
  `ip_hash` VARCHAR(64) DEFAULT NULL COMMENT 'IP哈希(不存原始IP)',
  `view_time` DATETIME NOT NULL COMMENT '访问时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_page` (`page_type`, `page_id`),
  INDEX `idx_visitor` (`visitor_id`),
  INDEX `idx_view_time` (`view_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SEO页面访问事件';

CALL `p_add_column`('seo_page_view', 'entity_id', 'BIGINT DEFAULT NULL COMMENT ''关联实体ID'' AFTER `page_id`');
CALL `p_add_column`('seo_page_view', 'page_path', 'VARCHAR(512) DEFAULT NULL COMMENT ''页面路径'' AFTER `page_slug`');
CALL `p_add_column`('seo_page_view', 'referer_source', 'VARCHAR(32) DEFAULT NULL COMMENT ''来源类型'' AFTER `referer`');
CALL `p_add_column`('seo_page_view', 'referer_url', 'VARCHAR(512) DEFAULT NULL COMMENT ''来源URL'' AFTER `referer_source`');
CALL `p_add_column`('seo_page_view', 'update_time', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL `p_add_column`('seo_page_view', 'deleted', 'TINYINT NOT NULL DEFAULT 0');

-- ============================================================
-- 清理存储过程
-- ============================================================

DROP PROCEDURE IF EXISTS `p_add_column`;
DROP PROCEDURE IF EXISTS `p_add_index`;
DROP PROCEDURE IF EXISTS `p_add_unique_index`;
DROP PROCEDURE IF EXISTS `p_drop_index`;
