-- ============================================================
-- Phase13: Growth & Enterprise Ecosystem — H2测试兼容版
-- 仅包含H2可执行的ALTER TABLE ADD COLUMN + CREATE TABLE语句
-- MySQL存储过程(p_add_column/p_add_index)在H2中自动跳过
-- ============================================================

-- P0: SEO Slug 列
ALTER TABLE robot ADD COLUMN slug VARCHAR(128);
ALTER TABLE brand ADD COLUMN slug VARCHAR(128);
ALTER TABLE company ADD COLUMN slug VARCHAR(128);
ALTER TABLE article ADD COLUMN slug VARCHAR(128);
ALTER TABLE robot_category ADD COLUMN slug VARCHAR(128);

-- P1: 企业认证状态列
ALTER TABLE company ADD COLUMN verification_status VARCHAR(20) DEFAULT 'UNCLAIMED';

-- P1: 企业认领申请表
CREATE TABLE IF NOT EXISTS `company_claim` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL,
  `applicant_user_id` BIGINT NOT NULL,
  `pending_company_id` BIGINT DEFAULT NULL,
  `pending_applicant_user_id` BIGINT DEFAULT NULL,
  `applicant_name` VARCHAR(64),
  `applicant_phone` VARCHAR(32),
  `applicant_email` VARCHAR(128),
  `applicant_position` VARCHAR(64),
  `claim_reason` VARCHAR(512),
  `proof_materials` TEXT,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `reviewer_user_id` BIGINT,
  `reviewer_name` VARCHAR(64),
  `review_time` DATETIME,
  `review_comment` VARCHAR(512),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE (`pending_company_id`),
  UNIQUE (`pending_applicant_user_id`)
);

-- P2: 企业资料变更申请表
CREATE TABLE IF NOT EXISTS `company_profile_change` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL,
  `submitter_user_id` BIGINT NOT NULL,
  `submitter_name` VARCHAR(64),
  `change_type` VARCHAR(32) NOT NULL,
  `field_name` VARCHAR(64) NOT NULL,
  `field_label` VARCHAR(64),
  `old_value` TEXT,
  `new_value` TEXT,
  `change_reason` VARCHAR(512),
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `reviewer_user_id` BIGINT,
  `reviewer_name` VARCHAR(64),
  `review_time` DATETIME,
  `review_comment` VARCHAR(512),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

-- P4: SEO页面访问事件表
CREATE TABLE IF NOT EXISTS `seo_page_view` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `page_type` VARCHAR(32) NOT NULL,
  `page_id` BIGINT,
  `entity_id` BIGINT,
  `page_slug` VARCHAR(128),
  `page_path` VARCHAR(512),
  `visitor_id` VARCHAR(64),
  `user_id` BIGINT,
  `referer` VARCHAR(512),
  `referer_source` VARCHAR(32),
  `referer_url` VARCHAR(512),
  `user_agent` VARCHAR(512),
  `ip_hash` VARCHAR(64),
  `view_time` DATETIME NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);
