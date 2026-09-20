-- ============================================================
-- Phase10 测试数据: Ecosystem Growth & Trust
-- H2兼容 / MySQL 5.6兼容
-- ============================================================

-- 0a. Phase10核心表兜底（生产SQL14经TestSqlSupport翻译后部分表可能未创建）

-- P0-1: robot_data_source
CREATE TABLE IF NOT EXISTS robot_data_source (
  id BIGINT NOT NULL AUTO_INCREMENT,
  robot_id BIGINT NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  source_name VARCHAR(255) NOT NULL,
  source_url VARCHAR(1024) DEFAULT NULL,
  trust_weight TINYINT NOT NULL DEFAULT 5,
  verified TINYINT NOT NULL DEFAULT 0,
  verified_time TIMESTAMP DEFAULT NULL,
  create_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

-- P0-2: robot_change_record
CREATE TABLE IF NOT EXISTS robot_change_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  robot_id BIGINT NOT NULL,
  change_type VARCHAR(32) NOT NULL,
  field_name VARCHAR(64) NOT NULL,
  field_label VARCHAR(128) DEFAULT NULL,
  old_value TEXT,
  new_value TEXT,
  source_type VARCHAR(32) DEFAULT NULL,
  source_name VARCHAR(255) DEFAULT NULL,
  verified TINYINT NOT NULL DEFAULT 0,
  event_key CHAR(64) DEFAULT NULL,
  change_time TIMESTAMP NOT NULL,
  create_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_robot_change_record_event_key UNIQUE (event_key)
);

-- P0-3: user_subscription
CREATE TABLE IF NOT EXISTS user_subscription (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  target_type VARCHAR(16) NOT NULL,
  target_id BIGINT NOT NULL,
  event_types VARCHAR(255) NOT NULL DEFAULT 'NEW_CONTENT,IMPORTANT_CHANGE',
  enabled TINYINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_user_subscription_user_target UNIQUE (user_id, target_type, target_id)
);

-- P0-6: company_member（安全修复核心表）
CREATE TABLE IF NOT EXISTS company_member (
  id BIGINT NOT NULL AUTO_INCREMENT,
  company_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(16) NOT NULL DEFAULT 'MEMBER',
  status TINYINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_company_member_company_user UNIQUE (company_id, user_id)
);

-- P0-6: procurement_response
CREATE TABLE IF NOT EXISTS procurement_response (
  id BIGINT NOT NULL AUTO_INCREMENT,
  procurement_id BIGINT NOT NULL,
  company_id BIGINT NOT NULL,
  contact_user_id BIGINT NOT NULL,
  solution TEXT,
  robot_ids VARCHAR(512) DEFAULT NULL,
  price_description VARCHAR(1024) DEFAULT NULL,
  delivery_description VARCHAR(512) DEFAULT NULL,
  contact_description VARCHAR(512) DEFAULT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'SUBMITTED',
  event_key CHAR(64) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_procurement_response_procurement_company UNIQUE (procurement_id, company_id)
);

-- P0-5: procurement_follow_record
CREATE TABLE IF NOT EXISTS procurement_follow_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  procurement_id BIGINT NOT NULL,
  operator_id BIGINT NOT NULL,
  operator_name VARCHAR(64) DEFAULT NULL,
  action VARCHAR(32) NOT NULL,
  content VARCHAR(1024) DEFAULT NULL,
  old_status VARCHAR(32) DEFAULT NULL,
  new_status VARCHAR(32) DEFAULT NULL,
  next_follow_time TIMESTAMP DEFAULT NULL,
  create_time TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id)
);

-- P0-7: user_reputation
CREATE TABLE IF NOT EXISTS user_reputation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  reputation_score INT NOT NULL DEFAULT 0,
  reputation_level VARCHAR(16) NOT NULL DEFAULT 'NEW',
  accepted_corrections INT NOT NULL DEFAULT 0,
  accepted_answers INT NOT NULL DEFAULT 0,
  helpful_reviews INT NOT NULL DEFAULT 0,
  quality_posts INT NOT NULL DEFAULT 0,
  penalty_score INT NOT NULL DEFAULT 0,
  daily_contrib_cap INT NOT NULL DEFAULT 50,
  update_time TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_user_reputation_user_id UNIQUE (user_id)
);

-- P0-7: reputation_event
CREATE TABLE IF NOT EXISTS reputation_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  event_type VARCHAR(32) NOT NULL,
  event_key CHAR(64) DEFAULT NULL,
  score_delta INT NOT NULL DEFAULT 0,
  reference_type VARCHAR(16) DEFAULT NULL,
  reference_id BIGINT DEFAULT NULL,
  create_time TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_reputation_event_event_key UNIQUE (event_key)
);

-- P1-4: user_collection
CREATE TABLE IF NOT EXISTS user_collection (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(512) DEFAULT NULL,
  visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE',
  robot_count INT NOT NULL DEFAULT 0,
  create_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

-- P1-4: user_collection_item
CREATE TABLE IF NOT EXISTS user_collection_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  collection_id BIGINT NOT NULL,
  robot_id BIGINT NOT NULL,
  note VARCHAR(255) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_collection_item_collection_robot UNIQUE (collection_id, robot_id)
);

-- P1-5: topic
CREATE TABLE IF NOT EXISTS topic (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  description VARCHAR(512) DEFAULT NULL,
  icon VARCHAR(255) DEFAULT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  post_count INT NOT NULL DEFAULT 0,
  create_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_topic_name UNIQUE (name)
);

-- ============================================================
-- ALTER TABLE ADD COLUMN (H2兼容 - 使用IF NOT EXISTS保护)
-- ============================================================

-- P0-1: robot表增加trust字段
ALTER TABLE robot ADD COLUMN IF NOT EXISTS trust_level VARCHAR(16) DEFAULT 'NORMAL';
ALTER TABLE robot ADD COLUMN IF NOT EXISTS trust_score INT NOT NULL DEFAULT 0;
ALTER TABLE robot ADD COLUMN IF NOT EXISTS pending_correction_count INT NOT NULL DEFAULT 0;

-- P0-4: message表增加字段
ALTER TABLE message ADD COLUMN IF NOT EXISTS notification_type VARCHAR(32) DEFAULT 'SYSTEM';
ALTER TABLE message ADD COLUMN IF NOT EXISTS target_type VARCHAR(16) DEFAULT NULL;
ALTER TABLE message ADD COLUMN IF NOT EXISTS target_id BIGINT DEFAULT NULL;
ALTER TABLE message ADD COLUMN IF NOT EXISTS summary VARCHAR(512) DEFAULT NULL;
ALTER TABLE message ADD COLUMN IF NOT EXISTS event_key CHAR(64) DEFAULT NULL;
ALTER TABLE message ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0;
ALTER TABLE message ADD COLUMN IF NOT EXISTS clicked TINYINT NOT NULL DEFAULT 0;

-- P0-5: inquiry表增加CRM字段
ALTER TABLE inquiry ADD COLUMN IF NOT EXISTS pipeline_status VARCHAR(32) DEFAULT 'NEW';
ALTER TABLE inquiry ADD COLUMN IF NOT EXISTS crm_owner BIGINT DEFAULT NULL;
ALTER TABLE inquiry ADD COLUMN IF NOT EXISTS crm_priority TINYINT DEFAULT 0;
ALTER TABLE inquiry ADD COLUMN IF NOT EXISTS next_follow_time TIMESTAMP DEFAULT NULL;
ALTER TABLE inquiry ADD COLUMN IF NOT EXISTS last_follow_time TIMESTAMP DEFAULT NULL;
ALTER TABLE inquiry ADD COLUMN IF NOT EXISTS crm_source VARCHAR(32) DEFAULT NULL;
ALTER TABLE inquiry ADD COLUMN IF NOT EXISTS crm_remark VARCHAR(1024) DEFAULT NULL;

-- P0-7: user表增加信誉字段
ALTER TABLE user ADD COLUMN IF NOT EXISTS reputation_score INT NOT NULL DEFAULT 0;
ALTER TABLE user ADD COLUMN IF NOT EXISTS reputation_level VARCHAR(16) DEFAULT 'NEW';

-- P0-8: growth_daily_stat增加新指标
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS returning_users INT NOT NULL DEFAULT 0;
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS subscription_count INT NOT NULL DEFAULT 0;
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS notification_sent INT NOT NULL DEFAULT 0;
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS notification_clicked INT NOT NULL DEFAULT 0;
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS contribution_users INT NOT NULL DEFAULT 0;
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS accepted_corrections INT NOT NULL DEFAULT 0;
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS procurement_leads INT NOT NULL DEFAULT 0;
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS enterprise_responses INT NOT NULL DEFAULT 0;
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS follow_conversions INT NOT NULL DEFAULT 0;

-- P1-5: community_post增加topic_id
ALTER TABLE community_post ADD COLUMN IF NOT EXISTS topic_id BIGINT DEFAULT NULL;

-- ============================================================
-- 测试数据
-- ============================================================

-- company_member: 测试用户(user_id=2)为企业(company_id=1)的OWNER
-- 注意: company表数据来自03_demo_data.sql/06_real_brands_companies.sql
INSERT IGNORE INTO company_member (company_id, user_id, role, status, create_time, update_time)
VALUES
(1, 2, 'OWNER', 1, NOW(), NOW()),
(1, 3, 'MEMBER', 1, NOW(), NOW()),
(2, 2, 'ADMIN', 1, NOW(), NOW());

-- user_reputation: 用户信誉测试数据
INSERT IGNORE INTO user_reputation (user_id, reputation_score, reputation_level, accepted_corrections, accepted_answers, helpful_reviews, quality_posts, penalty_score, daily_contrib_cap, update_time)
VALUES
(1, 120, 'TRUSTED', 3, 5, 8, 4, 0, 50, NOW()),
(2, 45, 'CONTRIBUTOR', 1, 2, 3, 1, 0, 50, NOW()),
(3, 10, 'NEW', 0, 0, 1, 0, 0, 50, NOW());

-- user_subscription:订阅测试数据
INSERT IGNORE INTO user_subscription (user_id, target_type, target_id, event_types, enabled, create_time, update_time)
VALUES
(2, 'ROBOT', 1, 'NEW_CONTENT,IMPORTANT_CHANGE', 1, NOW(), NOW()),
(2, 'BRAND', 1, 'NEW_CONTENT', 1, NOW(), NOW());

-- topic:话题测试数据
INSERT IGNORE INTO topic (name, description, sort_order, status, post_count, create_time, update_time)
VALUES
('行业动态', '机器人行业最新动态', 1, 1, 0, NOW(), NOW()),
('技术交流', '机器人技术交流讨论', 2, 1, 0, NOW(), NOW()),
('产品评测', '机器人产品评测分享', 3, 1, 0, NOW(), NOW());

-- robot_data_source:数据来源测试数据
INSERT IGNORE INTO robot_data_source (robot_id, source_type, source_name, source_url, trust_weight, verified, create_time, update_time)
VALUES
(1, 'OFFICIAL', '官方数据', NULL, 10, 1, NOW(), NOW()),
(1, 'VENDOR_SITE', '厂商官网', NULL, 8, 1, NOW(), NOW()),
(2, 'OFFICIAL', '官方数据', NULL, 10, 1, NOW(), NOW());