-- ============================================================
-- Phase9 测试数据: Product Growth & Ecosystem
-- H2兼容 / MySQL 5.6兼容
-- ============================================================

-- 0a. Phase6核心表兜底（生产SQL10经TestSqlSupport翻译后部分表可能未创建）
CREATE TABLE IF NOT EXISTS behavior_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT DEFAULT NULL,
  session_id VARCHAR(64) DEFAULT NULL,
  event_type VARCHAR(32) NOT NULL,
  biz_type VARCHAR(16) DEFAULT NULL,
  biz_id BIGINT DEFAULT NULL,
  extra VARCHAR(512) DEFAULT NULL,
  ip VARCHAR(64) DEFAULT NULL,
  user_agent VARCHAR(512) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ranking_snapshot (
  id BIGINT NOT NULL AUTO_INCREMENT,
  rank_type VARCHAR(32) NOT NULL,
  snapshot_date DATE NOT NULL,
  robot_id BIGINT NOT NULL,
  hot_score BIGINT DEFAULT 0,
  rank_no INT NOT NULL,
  prev_rank_no INT DEFAULT NULL,
  rank_change INT DEFAULT NULL,
  reason_code VARCHAR(64) DEFAULT NULL,
  reason_text VARCHAR(255) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_ranking_snapshot_type_date_robot UNIQUE (rank_type, snapshot_date, robot_id)
);

CREATE TABLE IF NOT EXISTS ranking_weight (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_type VARCHAR(32) NOT NULL,
  weight INT NOT NULL DEFAULT 0,
  description VARCHAR(128) DEFAULT NULL,
  create_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_ranking_weight_event UNIQUE (event_type)
);

CREATE TABLE IF NOT EXISTS ranking_decay_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  rank_type VARCHAR(32) NOT NULL,
  half_life_days INT NOT NULL DEFAULT 90,
  min_decay_factor DECIMAL(5,4) DEFAULT 0.1000,
  description VARCHAR(128) DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_ranking_decay_config_type UNIQUE (rank_type)
);

-- ranking_weight 初始数据
MERGE INTO ranking_weight (event_type, weight, description, create_time) KEY(event_type) VALUES
('VIEW', 1, '浏览', CURRENT_TIMESTAMP),
('FAVORITE', 8, '收藏', CURRENT_TIMESTAMP),
('COMPARE', 12, '对比', CURRENT_TIMESTAMP),
('INQUIRY', 30, '询价', CURRENT_TIMESTAMP),
('COMMENT', 5, '评论', CURRENT_TIMESTAMP),
('SCORE', 10, '评分', CURRENT_TIMESTAMP),
('NEW_PRODUCT', 2, '新品加权', CURRENT_TIMESTAMP),
('FOLLOW', 6, '关注', CURRENT_TIMESTAMP),
('REVIEW_CREATE', 15, '发布评价', CURRENT_TIMESTAMP),
('POST_CREATE', 4, '发帖', CURRENT_TIMESTAMP),
('QUESTION_CREATE', 3, '提问', CURRENT_TIMESTAMP),
('ANSWER_CREATE', 2, '回答', CURRENT_TIMESTAMP),
('BRAND_VIEW', 1, '品牌浏览', CURRENT_TIMESTAMP),
('COMPANY_VIEW', 1, '企业浏览', CURRENT_TIMESTAMP),
('SELECTION_SEARCH', 8, '选型搜索', CURRENT_TIMESTAMP);

-- ranking_decay_config 初始数据
MERGE INTO ranking_decay_config (rank_type, half_life_days, min_decay_factor, description, update_time) KEY(rank_type) VALUES
('hot', 90, 0.1000, '综合热度', CURRENT_TIMESTAMP),
('humanoid', 120, 0.1000, '人形机器人', CURRENT_TIMESTAMP),
('quadruped', 120, 0.1000, '四足机器人', CURRENT_TIMESTAMP),
('service', 90, 0.1000, '服务机器人', CURRENT_TIMESTAMP),
('industrial', 180, 0.0500, '工业机器人', CURRENT_TIMESTAMP),
('family', 90, 0.1000, '家庭机器人', CURRENT_TIMESTAMP),
('dev', 60, 0.1500, '开发平台', CURRENT_TIMESTAMP),
('follow', 90, 0.1000, '关注榜', CURRENT_TIMESTAMP),
('favorite', 60, 0.1000, '收藏榜', CURRENT_TIMESTAMP),
('discussion', 30, 0.1500, '讨论榜', CURRENT_TIMESTAMP),
('review', 120, 0.0800, '口碑榜', CURRENT_TIMESTAMP),
('new_product', 45, 0.2000, '新品榜', CURRENT_TIMESTAMP),
('company_attention', 90, 0.1000, '企业关注榜', CURRENT_TIMESTAMP);

-- 0b. H2兼容建表（生产SQL13经TestSqlSupport翻译后可能部分语句跳过，此处确保核心表存在）
CREATE TABLE IF NOT EXISTS user_contribution_stat (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  review_count INT NOT NULL DEFAULT 0,
  post_count INT NOT NULL DEFAULT 0,
  question_count INT NOT NULL DEFAULT 0,
  answer_count INT NOT NULL DEFAULT 0,
  helpful_received INT NOT NULL DEFAULT 0,
  correction_accepted INT NOT NULL DEFAULT 0,
  contribution_score INT NOT NULL DEFAULT 0,
  contributor_level TINYINT NOT NULL DEFAULT 0,
  update_time TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_user_contribution_stat_user_id UNIQUE (user_id)
);

CREATE TABLE IF NOT EXISTS growth_daily_stat (
  id BIGINT NOT NULL AUTO_INCREMENT,
  stat_date DATE NOT NULL,
  new_users INT NOT NULL DEFAULT 0,
  active_users INT NOT NULL DEFAULT 0,
  robot_views INT NOT NULL DEFAULT 0,
  searches INT NOT NULL DEFAULT 0,
  favorites INT NOT NULL DEFAULT 0,
  compares INT NOT NULL DEFAULT 0,
  questions INT NOT NULL DEFAULT 0,
  answers INT NOT NULL DEFAULT 0,
  posts INT NOT NULL DEFAULT 0,
  reviews INT NOT NULL DEFAULT 0,
  selections INT NOT NULL DEFAULT 0,
  inquiries INT NOT NULL DEFAULT 0,
  procurements INT NOT NULL DEFAULT 0,
  brand_views INT NOT NULL DEFAULT 0,
  company_views INT NOT NULL DEFAULT 0,
  follows INT NOT NULL DEFAULT 0,
  create_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_growth_daily_stat_stat_date UNIQUE (stat_date)
);

-- 1. 用户贡献统计测试数据
INSERT IGNORE INTO `user_contribution_stat` (`user_id`, `review_count`, `post_count`, `question_count`, `answer_count`, `helpful_received`, `correction_accepted`, `contribution_score`, `contributor_level`, `update_time`)
VALUES
(1, 5, 10, 3, 8, 20, 2, 89, 2, NOW()),
(2, 2, 5, 1, 3, 8, 0, 34, 1, NOW()),
(3, 0, 2, 0, 0, 0, 0, 6, 0, NOW());

-- 2. 排行榜新类型衰减配置测试数据（已在migration中INSERT IGNORE）
-- 确认ranking_weight新行存在

-- 3. 增长日统计测试数据
INSERT IGNORE INTO `growth_daily_stat` (`stat_date`, `new_users`, `active_users`, `robot_views`, `searches`, `favorites`, `compares`, `questions`, `answers`, `posts`, `reviews`, `selections`, `inquiries`, `procurements`, `brand_views`, `company_views`, `follows`, `create_time`, `update_time`)
VALUES
(DATEADD('DAY', -6, CURRENT_DATE), 15, 120, 850, 320, 45, 28, 8, 12, 15, 5, 20, 10, 3, 180, 90, 35, NOW(), NOW()),
(DATEADD('DAY', -5, CURRENT_DATE), 18, 135, 920, 350, 52, 31, 10, 15, 18, 6, 22, 12, 4, 195, 98, 40, NOW(), NOW()),
(DATEADD('DAY', -4, CURRENT_DATE), 12, 110, 780, 290, 38, 22, 6, 9, 12, 4, 18, 8, 2, 160, 82, 30, NOW(), NOW()),
(DATEADD('DAY', -3, CURRENT_DATE), 22, 150, 1050, 400, 60, 35, 12, 18, 22, 8, 25, 15, 5, 220, 110, 48, NOW(), NOW()),
(DATEADD('DAY', -2, CURRENT_DATE), 16, 128, 890, 340, 48, 29, 9, 14, 16, 5, 21, 11, 3, 185, 95, 38, NOW(), NOW()),
(DATEADD('DAY', -1, CURRENT_DATE), 20, 142, 980, 380, 55, 33, 11, 16, 20, 7, 24, 14, 4, 210, 105, 45, NOW(), NOW()),
(CURRENT_DATE, 8, 65, 420, 160, 22, 14, 4, 6, 8, 3, 10, 5, 1, 90, 45, 18, NOW(), NOW());

-- 4. 排行榜快照测试数据（含排名变化）
INSERT IGNORE INTO `ranking_snapshot` (`rank_type`, `snapshot_date`, `robot_id`, `hot_score`, `rank_no`, `prev_rank_no`, `rank_change`, `reason_code`, `reason_text`, `create_time`)
VALUES
('hot', DATEADD('DAY', -1, CURRENT_DATE), 1, 9500, 1, 1, 0, 'HIGH_ENGAGEMENT', '持续高热度', NOW()),
('hot', DATEADD('DAY', -1, CURRENT_DATE), 2, 8200, 2, 3, 1, 'FAVORITE_SURGE', '收藏增长较快', NOW()),
('hot', DATEADD('DAY', -1, CURRENT_DATE), 3, 7800, 3, 2, -1, 'STABLE', '热度稳定', NOW()),
('hot', DATEADD('DAY', -1, CURRENT_DATE), 4, 6500, 4, NULL, NULL, 'NEW_ENTRY', '新上榜', NOW()),
('follow', DATEADD('DAY', -1, CURRENT_DATE), 1, 320, 1, 1, 0, 'HIGH_FOLLOW', '关注数领先', NOW()),
('follow', DATEADD('DAY', -1, CURRENT_DATE), 2, 210, 2, 2, 0, 'STABLE', '关注稳定', NOW()),
('review', DATEADD('DAY', -1, CURRENT_DATE), 1, 450, 1, 1, 0, 'HIGH_REVIEW_SCORE', '口碑评分高', NOW()),
('new_product', DATEADD('DAY', -1, CURRENT_DATE), 5, 2800, 1, NULL, NULL, 'NEW_RELEASE', '近期新发布', NOW());