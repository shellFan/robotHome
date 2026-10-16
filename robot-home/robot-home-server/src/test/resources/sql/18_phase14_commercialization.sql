-- ============================================================
-- Phase14: Commercialization & Platform Operations — H2测试兼容版
-- 仅包含H2可执行的CREATE TABLE + INSERT语句
-- ============================================================

-- 1. enterprise_plan: 企业商业套餐
CREATE TABLE IF NOT EXISTS `enterprise_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(32) NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `description` VARCHAR(500),
  `level` INT NOT NULL DEFAULT 0,
  `display_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `duration_days` INT NOT NULL DEFAULT 365,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  `current_version` INT NOT NULL DEFAULT 0,
  `saleable` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE (`code`, `deleted`)
);

-- 2. enterprise_plan_version: 套餐版本
CREATE TABLE IF NOT EXISTS `enterprise_plan_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `plan_id` BIGINT NOT NULL,
  `version` INT NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `price_cents` BIGINT NOT NULL DEFAULT 0,
  `duration_days` INT NOT NULL DEFAULT 365,
  `entitlement_config` TEXT,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `effective_time` DATETIME,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE (`plan_id`, `version`, `deleted`)
);

-- 3. enterprise_subscription: 企业订阅
CREATE TABLE IF NOT EXISTS `enterprise_subscription` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL,
  `plan_id` BIGINT NOT NULL,
  `plan_version_id` BIGINT NOT NULL,
  `plan_name` VARCHAR(64) NOT NULL,
  `status` VARCHAR(24) NOT NULL DEFAULT 'FREE',
  `start_time` DATETIME,
  `end_time` DATETIME,
  `auto_renew` TINYINT NOT NULL DEFAULT 0,
  `source_order_id` BIGINT,
  `cancel_time` DATETIME,
  `suspend_reason` VARCHAR(200),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE (`company_id`, `deleted`)
);

-- 4. enterprise_entitlement: 企业权益实例
CREATE TABLE IF NOT EXISTS `enterprise_entitlement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL,
  `subscription_id` BIGINT NOT NULL,
  `ent_code` VARCHAR(64) NOT NULL,
  `ent_config` VARCHAR(500),
  `quota_total` INT NOT NULL DEFAULT 0,
  `quota_used` INT NOT NULL DEFAULT 0,
  `period_type` VARCHAR(16) NOT NULL DEFAULT 'MONTHLY',
  `period_start` DATETIME,
  `period_end` DATETIME,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `expire_time` DATETIME,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE (`company_id`, `ent_code`, `deleted`)
);

-- 5. enterprise_entitlement_usage: 权益使用记录
CREATE TABLE IF NOT EXISTS `enterprise_entitlement_usage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL,
  `entitlement_id` BIGINT NOT NULL,
  `ent_code` VARCHAR(64) NOT NULL,
  `usage_type` VARCHAR(32) NOT NULL,
  `reference_type` VARCHAR(32),
  `reference_id` BIGINT,
  `delta` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

-- 6. commercial_order: 商业订单
CREATE TABLE IF NOT EXISTS `commercial_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL,
  `company_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `plan_id` BIGINT NOT NULL,
  `plan_version_id` BIGINT NOT NULL,
  `plan_snapshot` TEXT,
  `original_price_cents` BIGINT NOT NULL DEFAULT 0,
  `payable_amount_cents` BIGINT NOT NULL DEFAULT 0,
  `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY',
  `status` VARCHAR(24) NOT NULL DEFAULT 'CREATED',
  `payment_channel` VARCHAR(32),
  `payment_no` VARCHAR(64),
  `idempotent_key` VARCHAR(64) NOT NULL,
  `pay_time` DATETIME,
  `close_time` DATETIME,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE (`order_no`),
  UNIQUE (`idempotent_key`)
);

-- 7. commercial_payment: 支付记录
CREATE TABLE IF NOT EXISTS `commercial_payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `order_no` VARCHAR(32) NOT NULL,
  `payment_channel` VARCHAR(32) NOT NULL,
  `channel_payment_no` VARCHAR(64),
  `amount_cents` BIGINT NOT NULL DEFAULT 0,
  `status` VARCHAR(24) NOT NULL DEFAULT 'PENDING',
  `callback_raw` TEXT,
  `verify_result` VARCHAR(16),
  `callback_time` DATETIME,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

-- 8. commercial_refund: 退款记录
CREATE TABLE IF NOT EXISTS `commercial_refund` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `refund_no` VARCHAR(32) NOT NULL,
  `amount_cents` BIGINT NOT NULL DEFAULT 0,
  `status` VARCHAR(24) NOT NULL DEFAULT 'PENDING',
  `reason` VARCHAR(500),
  `operator_id` BIGINT,
  `confirm_time` DATETIME,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE (`refund_no`)
);

-- 9. advertising_slot: 广告位
CREATE TABLE IF NOT EXISTS `advertising_slot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `slot_code` VARCHAR(32) NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `page_location` VARCHAR(128),
  `size_hint` VARCHAR(64),
  `allowed_types` VARCHAR(128) DEFAULT 'IMAGE',
  `rules` VARCHAR(500),
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `sort` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE (`slot_code`, `deleted`)
);

-- 10. advertising_campaign: 广告活动
CREATE TABLE IF NOT EXISTS `advertising_campaign` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL,
  `slot_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `status` VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  `start_time` DATETIME,
  `end_time` DATETIME,
  `budget_cents` BIGINT NOT NULL DEFAULT 0,
  `daily_limit` INT NOT NULL DEFAULT 0,
  `total_limit` INT NOT NULL DEFAULT 0,
  `reviewer_id` BIGINT,
  `review_time` DATETIME,
  `review_note` VARCHAR(500),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

-- 11. advertising_creative: 广告素材
CREATE TABLE IF NOT EXISTS `advertising_creative` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `campaign_id` BIGINT NOT NULL,
  `media_type` VARCHAR(16) NOT NULL DEFAULT 'IMAGE',
  `media_url` VARCHAR(500) NOT NULL,
  `jump_url` VARCHAR(500),
  `title` VARCHAR(128),
  `description` VARCHAR(500),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

-- 12. advertising_event: 广告事件
CREATE TABLE IF NOT EXISTS `advertising_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `campaign_id` BIGINT NOT NULL,
  `slot_id` BIGINT NOT NULL,
  `event_type` VARCHAR(16) NOT NULL,
  `visitor_id` VARCHAR(64),
  `ip` VARCHAR(64),
  `dedup_key` VARCHAR(128) NOT NULL,
  `event_time` DATETIME NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE (`dedup_key`)
);

-- 13. lead_recommendation: 商机推荐
CREATE TABLE IF NOT EXISTS `lead_recommendation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL,
  `procurement_id` BIGINT NOT NULL,
  `score` INT NOT NULL DEFAULT 0,
  `reason` VARCHAR(500),
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  `recommended_time` DATETIME NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE (`company_id`, `procurement_id`, `deleted`)
);

-- 14. commercial_audit_log: 商业审计日志
CREATE TABLE IF NOT EXISTS `commercial_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `operator_id` BIGINT,
  `operator_type` VARCHAR(16) NOT NULL DEFAULT 'USER',
  `company_id` BIGINT,
  `action` VARCHAR(64) NOT NULL,
  `entity_type` VARCHAR(32) NOT NULL,
  `entity_id` BIGINT,
  `detail` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

-- ============================================================
-- 初始数据：默认套餐 + 默认广告位
-- ============================================================

INSERT INTO `enterprise_plan` (`code`, `name`, `description`, `level`, `display_price`, `duration_days`, `sort`, `status`, `current_version`, `saleable`)
SELECT 'FREE', '免费版', '基础企业能力', 0, 0.00, 36500, 0, 'ON_SHELF', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `enterprise_plan` WHERE `code`='FREE' AND `deleted`=0);

INSERT INTO `enterprise_plan` (`code`, `name`, `description`, `level`, `display_price`, `duration_days`, `sort`, `status`, `current_version`, `saleable`)
SELECT 'BASIC', '企业基础版', '基础商业能力', 10, 0.00, 365, 10, 'DRAFT', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM `enterprise_plan` WHERE `code`='BASIC' AND `deleted`=0);

INSERT INTO `enterprise_plan` (`code`, `name`, `description`, `level`, `display_price`, `duration_days`, `sort`, `status`, `current_version`, `saleable`)
SELECT 'PRO', '企业专业版', '专业商业能力', 20, 0.00, 365, 20, 'DRAFT', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM `enterprise_plan` WHERE `code`='PRO' AND `deleted`=0);

INSERT INTO `enterprise_plan` (`code`, `name`, `description`, `level`, `display_price`, `duration_days`, `sort`, `status`, `current_version`, `saleable`)
SELECT 'ENTERPRISE', '企业旗舰版', '旗舰商业能力', 30, 0.00, 365, 30, 'DRAFT', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM `enterprise_plan` WHERE `code`='ENTERPRISE' AND `deleted`=0);

-- FREE套餐版本
INSERT INTO `enterprise_plan_version` (`plan_id`, `version`, `name`, `price_cents`, `duration_days`, `entitlement_config`, `status`)
SELECT p.`id`, 1, '免费版V1', 0, 36500,
  '[{"code":"LEAD_VIEW","quotaTotal":5,"periodType":"MONTHLY"},{"code":"LEAD_RESPONSE","quotaTotal":2,"periodType":"MONTHLY"}]',
  'ACTIVE'
FROM `enterprise_plan` p
WHERE p.`code`='FREE' AND p.`deleted`=0
  AND NOT EXISTS (SELECT 1 FROM `enterprise_plan_version` v WHERE v.`plan_id`=p.`id` AND v.`version`=1 AND v.`deleted`=0);

-- BASIC套餐版本
INSERT INTO `enterprise_plan_version` (`plan_id`, `version`, `name`, `price_cents`, `duration_days`, `entitlement_config`, `status`)
SELECT p.`id`, 1, '企业基础版V1', 0, 365,
  '[{"code":"LEAD_VIEW","quotaTotal":20,"periodType":"MONTHLY"},{"code":"LEAD_RESPONSE","quotaTotal":10,"periodType":"MONTHLY"},{"code":"PRODUCT_MANAGEMENT","quotaTotal":50,"periodType":"MONTHLY"},{"code":"COMPANY_PROFILE_ENHANCED","quotaTotal":0,"periodType":"TOTAL"}]',
  'ACTIVE'
FROM `enterprise_plan` p
WHERE p.`code`='BASIC' AND p.`deleted`=0
  AND NOT EXISTS (SELECT 1 FROM `enterprise_plan_version` v WHERE v.`plan_id`=p.`id` AND v.`version`=1 AND v.`deleted`=0);

-- PRO套餐版本
INSERT INTO `enterprise_plan_version` (`plan_id`, `version`, `name`, `price_cents`, `duration_days`, `entitlement_config`, `status`)
SELECT p.`id`, 1, '企业专业版V1', 0, 365,
  '[{"code":"LEAD_VIEW","quotaTotal":100,"periodType":"MONTHLY"},{"code":"LEAD_RESPONSE","quotaTotal":50,"periodType":"MONTHLY"},{"code":"PRODUCT_MANAGEMENT","quotaTotal":200,"periodType":"MONTHLY"},{"code":"PRODUCT_FEATURED","quotaTotal":10,"periodType":"MONTHLY"},{"code":"AD_CAMPAIGN","quotaTotal":3,"periodType":"MONTHLY"},{"code":"ANALYTICS_ADVANCED","quotaTotal":0,"periodType":"TOTAL"},{"code":"COMPANY_PROFILE_ENHANCED","quotaTotal":0,"periodType":"TOTAL"}]',
  'ACTIVE'
FROM `enterprise_plan` p
WHERE p.`code`='PRO' AND p.`deleted`=0
  AND NOT EXISTS (SELECT 1 FROM `enterprise_plan_version` v WHERE v.`plan_id`=p.`id` AND v.`version`=1 AND v.`deleted`=0);

-- ENTERPRISE套餐版本
INSERT INTO `enterprise_plan_version` (`plan_id`, `version`, `name`, `price_cents`, `duration_days`, `entitlement_config`, `status`)
SELECT p.`id`, 1, '企业旗舰版V1', 0, 365,
  '[{"code":"LEAD_VIEW","quotaTotal":0,"periodType":"MONTHLY"},{"code":"LEAD_RESPONSE","quotaTotal":0,"periodType":"MONTHLY"},{"code":"PRODUCT_MANAGEMENT","quotaTotal":0,"periodType":"MONTHLY"},{"code":"PRODUCT_FEATURED","quotaTotal":0,"periodType":"MONTHLY"},{"code":"AD_CAMPAIGN","quotaTotal":0,"periodType":"MONTHLY"},{"code":"ANALYTICS_ADVANCED","quotaTotal":0,"periodType":"TOTAL"},{"code":"COMPANY_PROFILE_ENHANCED","quotaTotal":0,"periodType":"TOTAL"}]',
  'ACTIVE'
FROM `enterprise_plan` p
WHERE p.`code`='ENTERPRISE' AND p.`deleted`=0
  AND NOT EXISTS (SELECT 1 FROM `enterprise_plan_version` v WHERE v.`plan_id`=p.`id` AND v.`version`=1 AND v.`deleted`=0);

-- 默认广告位
INSERT INTO `advertising_slot` (`slot_code`, `name`, `page_location`, `size_hint`, `allowed_types`, `status`, `sort`)
SELECT 'HOME_BANNER', '首页Banner', '首页顶部轮播', '1200x400', 'IMAGE', 'ACTIVE', 0
WHERE NOT EXISTS (SELECT 1 FROM `advertising_slot` WHERE `slot_code`='HOME_BANNER' AND `deleted`=0);

INSERT INTO `advertising_slot` (`slot_code`, `name`, `page_location`, `size_hint`, `allowed_types`, `status`, `sort`)
SELECT 'ROBOT_LIST', '机器人列表侧栏', '机器人列表页右侧', '300x250', 'IMAGE', 'ACTIVE', 10
WHERE NOT EXISTS (SELECT 1 FROM `advertising_slot` WHERE `slot_code`='ROBOT_LIST' AND `deleted`=0);

INSERT INTO `advertising_slot` (`slot_code`, `name`, `page_location`, `size_hint`, `allowed_types`, `status`, `sort`)
SELECT 'ROBOT_DETAIL', '机器人详情底部', '机器人详情页底部', '728x90', 'IMAGE', 'ACTIVE', 20
WHERE NOT EXISTS (SELECT 1 FROM `advertising_slot` WHERE `slot_code`='ROBOT_DETAIL' AND `deleted`=0);

INSERT INTO `advertising_slot` (`slot_code`, `name`, `page_location`, `size_hint`, `allowed_types`, `status`, `sort`)
SELECT 'COMPANY_PAGE', '企业页侧栏', '企业详情页右侧', '300x250', 'IMAGE', 'ACTIVE', 30
WHERE NOT EXISTS (SELECT 1 FROM `advertising_slot` WHERE `slot_code`='COMPANY_PAGE' AND `deleted`=0);

INSERT INTO `advertising_slot` (`slot_code`, `name`, `page_location`, `size_hint`, `allowed_types`, `status`, `sort`)
SELECT 'ARTICLE_DETAIL', '文章详情底部', '文章详情页底部', '728x90', 'IMAGE', 'ACTIVE', 40
WHERE NOT EXISTS (SELECT 1 FROM `advertising_slot` WHERE `slot_code`='ARTICLE_DETAIL' AND `deleted`=0);

INSERT INTO `advertising_slot` (`slot_code`, `name`, `page_location`, `size_hint`, `allowed_types`, `status`, `sort`)
SELECT 'SEARCH_RESULT', '搜索结果顶部', '搜索结果页顶部', '728x90', 'IMAGE', 'ACTIVE', 50
WHERE NOT EXISTS (SELECT 1 FROM `advertising_slot` WHERE `slot_code`='SEARCH_RESULT' AND `deleted`=0);

-- Phase14: 商业化运营权限
INSERT INTO `sys_permission` (`permission_code`, `permission_name`, `description`, `create_time`, `update_time`, `deleted`)
SELECT 'commercial:plan:manage', '套餐管理', '商业化套餐管理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='commercial:plan:manage' AND `deleted`=0);

INSERT INTO `sys_permission` (`permission_code`, `permission_name`, `description`, `create_time`, `update_time`, `deleted`)
SELECT 'commercial:order:manage', '订单管理', '商业化订单管理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='commercial:order:manage' AND `deleted`=0);

INSERT INTO `sys_permission` (`permission_code`, `permission_name`, `description`, `create_time`, `update_time`, `deleted`)
SELECT 'commercial:refund:manage', '退款管理', '商业化退款管理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='commercial:refund:manage' AND `deleted`=0);

INSERT INTO `sys_permission` (`permission_code`, `permission_name`, `description`, `create_time`, `update_time`, `deleted`)
SELECT 'commercial:subscription:manage', '订阅管理', '商业化订阅管理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='commercial:subscription:manage' AND `deleted`=0);

INSERT INTO `sys_permission` (`permission_code`, `permission_name`, `description`, `create_time`, `update_time`, `deleted`)
SELECT 'advertising:slot:manage', '广告位管理', '广告位管理', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='advertising:slot:manage' AND `deleted`=0);

INSERT INTO `sys_permission` (`permission_code`, `permission_name`, `description`, `create_time`, `update_time`, `deleted`)
SELECT 'advertising:campaign:review', '广告审核', '广告投放计划审核', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='advertising:campaign:review' AND `deleted`=0);