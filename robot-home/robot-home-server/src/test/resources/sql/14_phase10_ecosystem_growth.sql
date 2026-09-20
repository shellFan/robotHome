-- ============================================================
-- Phase10 Test Migration (H2 Compatible)
-- ============================================================

-- P0-1 Robot Data Trust
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
  PRIMARY KEY (id)
);

-- P0-2 Change History
CREATE TABLE IF NOT EXISTS robot_change_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  robot_id BIGINT NOT NULL,
  change_type VARCHAR(32) NOT NULL,
  field_name VARCHAR(64) NOT NULL,
  field_label VARCHAR(128) DEFAULT NULL,
  old_value CLOB DEFAULT NULL,
  new_value CLOB DEFAULT NULL,
  source_type VARCHAR(32) DEFAULT NULL,
  source_name VARCHAR(255) DEFAULT NULL,
  verified TINYINT NOT NULL DEFAULT 0,
  event_key CHAR(64) DEFAULT NULL,
  change_time TIMESTAMP NOT NULL,
  create_time TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_event_key UNIQUE (event_key)
);

-- P0-3 Subscription
CREATE TABLE IF NOT EXISTS user_subscription (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  target_type VARCHAR(16) NOT NULL,
  target_id BIGINT NOT NULL,
  event_types VARCHAR(255) NOT NULL DEFAULT 'NEW_CONTENT,IMPORTANT_CHANGE',
  enabled TINYINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_user_target UNIQUE (user_id, target_type, target_id)
);

-- P0-4 Notification Center 2.0 - message表新增字段
ALTER TABLE message ADD COLUMN IF NOT EXISTS notification_type VARCHAR(32) DEFAULT 'SYSTEM';
ALTER TABLE message ADD COLUMN IF NOT EXISTS target_type VARCHAR(16) DEFAULT NULL;
ALTER TABLE message ADD COLUMN IF NOT EXISTS target_id BIGINT DEFAULT NULL;
ALTER TABLE message ADD COLUMN IF NOT EXISTS summary VARCHAR(512) DEFAULT NULL;
ALTER TABLE message ADD COLUMN IF NOT EXISTS event_key CHAR(64) DEFAULT NULL;
ALTER TABLE message ADD COLUMN IF NOT EXISTS deleted TINYINT NOT NULL DEFAULT 0;

-- P0-5 Procurement CRM
ALTER TABLE inquiry ADD COLUMN IF NOT EXISTS pipeline_status VARCHAR(32) DEFAULT 'NEW';
ALTER TABLE inquiry ADD COLUMN IF NOT EXISTS crm_owner BIGINT DEFAULT NULL;
ALTER TABLE inquiry ADD COLUMN IF NOT EXISTS crm_priority TINYINT DEFAULT 0;
ALTER TABLE inquiry ADD COLUMN IF NOT EXISTS next_follow_time TIMESTAMP DEFAULT NULL;
ALTER TABLE inquiry ADD COLUMN IF NOT EXISTS last_follow_time TIMESTAMP DEFAULT NULL;
ALTER TABLE inquiry ADD COLUMN IF NOT EXISTS crm_source VARCHAR(32) DEFAULT NULL;
ALTER TABLE inquiry ADD COLUMN IF NOT EXISTS crm_remark VARCHAR(1024) DEFAULT NULL;

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

-- P0-6 Enterprise Response
CREATE TABLE IF NOT EXISTS procurement_response (
  id BIGINT NOT NULL AUTO_INCREMENT,
  procurement_id BIGINT NOT NULL,
  company_id BIGINT NOT NULL,
  contact_user_id BIGINT NOT NULL,
  solution CLOB DEFAULT NULL,
  robot_ids VARCHAR(512) DEFAULT NULL,
  price_description VARCHAR(1024) DEFAULT NULL,
  delivery_description VARCHAR(512) DEFAULT NULL,
  contact_description VARCHAR(512) DEFAULT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'SUBMITTED',
  event_key CHAR(64) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_procurement_company UNIQUE (procurement_id, company_id)
);

-- P0-7 Contribution Reputation
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
  CONSTRAINT uk_user_id UNIQUE (user_id)
);

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
  CONSTRAINT uk_event_key UNIQUE (event_key)
);

-- P0-8 Ecosystem Analytics - growth_daily_stat新增字段
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS returning_users INT NOT NULL DEFAULT 0;
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS subscription_count INT NOT NULL DEFAULT 0;
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS notification_sent INT NOT NULL DEFAULT 0;
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS notification_clicked INT NOT NULL DEFAULT 0;
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS contribution_users INT NOT NULL DEFAULT 0;
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS accepted_corrections INT NOT NULL DEFAULT 0;
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS procurement_leads INT NOT NULL DEFAULT 0;
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS enterprise_responses INT NOT NULL DEFAULT 0;
ALTER TABLE growth_daily_stat ADD COLUMN IF NOT EXISTS follow_conversions INT NOT NULL DEFAULT 0;

-- Robot表新增trust字段
ALTER TABLE robot ADD COLUMN IF NOT EXISTS trust_level VARCHAR(16) DEFAULT 'NORMAL';
ALTER TABLE robot ADD COLUMN IF NOT EXISTS trust_score INT NOT NULL DEFAULT 0;
ALTER TABLE robot ADD COLUMN IF NOT EXISTS pending_correction_count INT NOT NULL DEFAULT 0;

-- User表新增reputation字段
ALTER TABLE user ADD COLUMN IF NOT EXISTS reputation_score INT NOT NULL DEFAULT 0;
ALTER TABLE user ADD COLUMN IF NOT EXISTS reputation_level VARCHAR(16) DEFAULT 'NEW';

-- P1-4 Collections
CREATE TABLE IF NOT EXISTS user_collection (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(512) DEFAULT NULL,
  visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE',
  robot_count INT NOT NULL DEFAULT 0,
  create_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS user_collection_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  collection_id BIGINT NOT NULL,
  robot_id BIGINT NOT NULL,
  note VARCHAR(255) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_collection_robot UNIQUE (collection_id, robot_id)
);

-- P1-5 Topic
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
  PRIMARY KEY (id),
  CONSTRAINT uk_name UNIQUE (name)
);

-- community_post新增topic_id
ALTER TABLE community_post ADD COLUMN IF NOT EXISTS topic_id BIGINT DEFAULT NULL;