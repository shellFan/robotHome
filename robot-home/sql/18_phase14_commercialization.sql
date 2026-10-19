-- ============================================================
-- Phase14: Commercialization & Platform Operations 数据库迁移
-- 从Phase13 DB执行此脚本必须成功
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

DELIMITER ;

-- ============================================================
-- 1. enterprise_plan: 企业商业套餐
-- ============================================================
CREATE TABLE IF NOT EXISTS `enterprise_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(32) NOT NULL COMMENT '套餐编码: FREE/BASIC/PRO/ENTERPRISE',
  `name` VARCHAR(64) NOT NULL COMMENT '套餐名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '套餐描述',
  `level` INT NOT NULL DEFAULT 0 COMMENT '套餐等级(越大越高)',
  `display_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '展示价格(仅展示,实际以版本为准)',
  `duration_days` INT NOT NULL DEFAULT 365 COMMENT '默认有效期天数',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '展示排序',
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/ON_SHELF/OFF_SHELF',
  `current_version` INT NOT NULL DEFAULT 0 COMMENT '当前版本号',
  `saleable` TINYINT NOT NULL DEFAULT 0 COMMENT '可售: 0否 1是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_code` (`code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='企业商业套餐';

-- ============================================================
-- 2. enterprise_plan_version: 套餐版本(权益快照)
-- ============================================================
CREATE TABLE IF NOT EXISTS `enterprise_plan_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `plan_id` BIGINT NOT NULL COMMENT '套餐ID',
  `version` INT NOT NULL COMMENT '版本号',
  `name` VARCHAR(64) NOT NULL COMMENT '版本名称快照',
  `price_cents` BIGINT NOT NULL DEFAULT 0 COMMENT '版本价格(分)',
  `duration_days` INT NOT NULL DEFAULT 365 COMMENT '有效期天数',
  `entitlement_config` TEXT COMMENT '权益配置JSON',
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/ARCHIVED',
  `effective_time` DATETIME DEFAULT NULL COMMENT '生效时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_version` (`plan_id`, `version`, `deleted`),
  KEY `idx_plan_status` (`plan_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='套餐版本快照';

-- ============================================================
-- 3. enterprise_subscription: 企业订阅
-- ============================================================
CREATE TABLE IF NOT EXISTS `enterprise_subscription` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL COMMENT '企业ID',
  `plan_id` BIGINT NOT NULL COMMENT '套餐ID',
  `plan_version_id` BIGINT NOT NULL COMMENT '套餐版本ID',
  `plan_name` VARCHAR(64) NOT NULL COMMENT '套餐名称快照',
  `status` VARCHAR(24) NOT NULL DEFAULT 'FREE' COMMENT '状态: FREE/ACTIVE/EXPIRING/EXPIRED/CANCELLED/SUSPENDED',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '到期时间',
  `auto_renew` TINYINT NOT NULL DEFAULT 0 COMMENT '自动续费标记(仅标记,不自动扣款)',
  `source_order_id` BIGINT DEFAULT NULL COMMENT '来源订单ID',
  `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
  `suspend_reason` VARCHAR(200) DEFAULT NULL COMMENT '暂停原因',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_subscription` (`company_id`, `deleted`),
  KEY `idx_sub_status` (`status`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='企业订阅';

-- ============================================================
-- 4. enterprise_entitlement: 企业权益实例
-- ============================================================
CREATE TABLE IF NOT EXISTS `enterprise_entitlement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL COMMENT '企业ID',
  `subscription_id` BIGINT NOT NULL COMMENT '订阅ID',
  `ent_code` VARCHAR(64) NOT NULL COMMENT '权益编码',
  `ent_config` VARCHAR(500) DEFAULT NULL COMMENT '权益配置JSON',
  `quota_total` INT NOT NULL DEFAULT 0 COMMENT '总额度(0=无限)',
  `quota_used` INT NOT NULL DEFAULT 0 COMMENT '已用额度',
  `period_type` VARCHAR(16) NOT NULL DEFAULT 'MONTHLY' COMMENT '额度周期: MONTHLY/TOTAL',
  `period_start` DATETIME DEFAULT NULL COMMENT '周期开始',
  `period_end` DATETIME DEFAULT NULL COMMENT '周期结束',
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/EXPIRED/REVOKED',
  `expire_time` DATETIME DEFAULT NULL COMMENT '权益到期时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_ent` (`company_id`, `ent_code`, `deleted`),
  KEY `idx_ent_status` (`company_id`, `status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='企业权益实例';

-- ============================================================
-- 5. enterprise_entitlement_usage: 权益使用记录
-- ============================================================
CREATE TABLE IF NOT EXISTS `enterprise_entitlement_usage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL COMMENT '企业ID',
  `entitlement_id` BIGINT NOT NULL COMMENT '权益ID',
  `ent_code` VARCHAR(64) NOT NULL COMMENT '权益编码',
  `usage_type` VARCHAR(32) NOT NULL COMMENT '使用类型: CONSUME/RESERVE/RELEASE/RESET',
  `reference_type` VARCHAR(32) DEFAULT NULL COMMENT '关联类型',
  `reference_id` BIGINT DEFAULT NULL COMMENT '关联ID',
  `delta` INT NOT NULL DEFAULT 0 COMMENT '额度变化(正消耗/负释放)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_usage_company` (`company_id`, `ent_code`),
  KEY `idx_usage_ent` (`entitlement_id`),
  KEY `idx_usage_ref` (`reference_type`, `reference_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='权益使用记录';

-- ============================================================
-- 6. commercial_order: 商业订单
-- ============================================================
CREATE TABLE IF NOT EXISTS `commercial_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL COMMENT '订单号(唯一)',
  `company_id` BIGINT NOT NULL COMMENT '企业ID',
  `user_id` BIGINT NOT NULL COMMENT '下单用户ID',
  `plan_id` BIGINT NOT NULL COMMENT '套餐ID',
  `plan_version_id` BIGINT NOT NULL COMMENT '套餐版本ID',
  `plan_snapshot` TEXT COMMENT '套餐快照JSON',
  `original_price_cents` BIGINT NOT NULL DEFAULT 0 COMMENT '原价(分)',
  `payable_amount_cents` BIGINT NOT NULL DEFAULT 0 COMMENT '应付金额(分)',
  `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `status` VARCHAR(24) NOT NULL DEFAULT 'CREATED' COMMENT '状态: CREATED/PENDING_PAYMENT/PAID/CANCELLED/EXPIRED/REFUND_PENDING/REFUNDED/FAILED',
  `payment_channel` VARCHAR(32) DEFAULT NULL COMMENT '支付渠道',
  `payment_no` VARCHAR(64) DEFAULT NULL COMMENT '支付流水号',
  `idempotent_key` VARCHAR(64) NOT NULL COMMENT '幂等键',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `close_time` DATETIME DEFAULT NULL COMMENT '关闭时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_order_idem` (`idempotent_key`),
  KEY `idx_order_company` (`company_id`, `status`),
  KEY `idx_order_user` (`user_id`),
  KEY `idx_order_status` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商业订单';

-- ============================================================
-- 7. commercial_payment: 支付记录
-- ============================================================
CREATE TABLE IF NOT EXISTS `commercial_payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
  `payment_channel` VARCHAR(32) NOT NULL COMMENT '支付渠道: MOCK_SANDBOX/WECHAT/ALIPAY',
  `channel_payment_no` VARCHAR(64) DEFAULT NULL COMMENT '渠道支付号',
  `amount_cents` BIGINT NOT NULL DEFAULT 0 COMMENT '支付金额(分)',
  `status` VARCHAR(24) NOT NULL DEFAULT 'INIT' COMMENT '状态: INIT/SUCCESS/FAIL',
  `callback_raw` TEXT COMMENT '回调原始数据',
  `verify_result` VARCHAR(16) DEFAULT NULL COMMENT '验签结果: PASS/FAIL',
  `callback_time` DATETIME DEFAULT NULL COMMENT '回调时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_pay_order` (`order_id`),
  KEY `idx_pay_channel` (`channel_payment_no`),
  KEY `idx_pay_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支付记录';

-- ============================================================
-- 8. commercial_refund: 退款记录
-- ============================================================
CREATE TABLE IF NOT EXISTS `commercial_refund` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `refund_no` VARCHAR(32) NOT NULL COMMENT '退款号(唯一)',
  `amount_cents` BIGINT NOT NULL DEFAULT 0 COMMENT '退款金额(分)',
  `status` VARCHAR(24) NOT NULL DEFAULT 'APPLYING' COMMENT '状态: APPLYING/SUCCESS/FAIL',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '退款原因',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `confirm_time` DATETIME DEFAULT NULL COMMENT '确认时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_refund_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='退款记录';

-- ============================================================
-- 9. advertising_slot: 广告位
-- ============================================================
CREATE TABLE IF NOT EXISTS `advertising_slot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `slot_code` VARCHAR(32) NOT NULL COMMENT '广告位编码: HOME_BANNER/ROBOT_LIST/ROBOT_DETAIL等',
  `name` VARCHAR(64) NOT NULL COMMENT '广告位名称',
  `page_location` VARCHAR(128) DEFAULT NULL COMMENT '页面位置描述',
  `size_hint` VARCHAR(64) DEFAULT NULL COMMENT '尺寸建议',
  `allowed_types` VARCHAR(128) DEFAULT 'IMAGE' COMMENT '允许素材类型: IMAGE/VIDEO',
  `rules` VARCHAR(500) DEFAULT NULL COMMENT '投放规则JSON',
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
  `sort` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_slot_code` (`slot_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='广告位';

-- ============================================================
-- 10. advertising_campaign: 广告活动
-- ============================================================
CREATE TABLE IF NOT EXISTS `advertising_campaign` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL COMMENT '企业ID',
  `slot_id` BIGINT NOT NULL COMMENT '广告位ID',
  `name` VARCHAR(128) NOT NULL COMMENT '活动名称',
  `status` VARCHAR(24) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PENDING_REVIEW/APPROVED/REJECTED/ONLINE/OFFLINE',
  `start_time` DATETIME DEFAULT NULL COMMENT '投放开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '投放结束时间',
  `budget_cents` BIGINT NOT NULL DEFAULT 0 COMMENT '预算(分,沙箱)',
  `daily_limit` INT NOT NULL DEFAULT 0 COMMENT '每日展示上限(0=无限)',
  `total_limit` INT NOT NULL DEFAULT 0 COMMENT '总展示上限(0=无限)',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `review_note` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_camp_company` (`company_id`, `status`),
  KEY `idx_camp_slot` (`slot_id`, `status`),
  KEY `idx_camp_review` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='广告活动';

-- ============================================================
-- 11. advertising_creative: 广告素材
-- ============================================================
CREATE TABLE IF NOT EXISTS `advertising_creative` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `campaign_id` BIGINT NOT NULL COMMENT '活动ID',
  `media_type` VARCHAR(16) NOT NULL DEFAULT 'IMAGE' COMMENT '素材类型: IMAGE/VIDEO',
  `media_url` VARCHAR(500) NOT NULL COMMENT '素材URL',
  `jump_url` VARCHAR(500) DEFAULT NULL COMMENT '跳转链接',
  `title` VARCHAR(128) DEFAULT NULL COMMENT '广告标题',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '广告描述',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_creative_campaign` (`campaign_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='广告素材';

-- ============================================================
-- 12. advertising_event: 广告事件
-- ============================================================
CREATE TABLE IF NOT EXISTS `advertising_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `campaign_id` BIGINT NOT NULL COMMENT '活动ID',
  `slot_id` BIGINT NOT NULL COMMENT '广告位ID',
  `event_type` VARCHAR(16) NOT NULL COMMENT '事件类型: IMPRESSION/CLICK',
  `visitor_id` VARCHAR(64) DEFAULT NULL COMMENT '访客标识',
  `ip` VARCHAR(64) DEFAULT NULL COMMENT 'IP',
  `dedup_key` VARCHAR(128) NOT NULL COMMENT '去重键',
  `event_time` DATETIME NOT NULL COMMENT '事件时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_dedup` (`dedup_key`),
  KEY `idx_event_camp` (`campaign_id`, `event_type`),
  KEY `idx_event_slot` (`slot_id`, `event_type`, `event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='广告事件';

-- ============================================================
-- 13. lead_recommendation: 商机推荐
-- ============================================================
CREATE TABLE IF NOT EXISTS `lead_recommendation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL COMMENT '推荐给的企业ID',
  `procurement_id` BIGINT NOT NULL COMMENT '采购需求ID(inquiry.id)',
  `score` INT NOT NULL DEFAULT 0 COMMENT '推荐分数',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '推荐理由',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/VIEWED/RESPONDED/IGNORED',
  `recommended_time` DATETIME NOT NULL COMMENT '推荐时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_proc` (`company_id`, `procurement_id`, `deleted`),
  KEY `idx_rec_status` (`company_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商机推荐';

-- ============================================================
-- 14. commercial_audit_log: 商业审计日志
-- ============================================================
CREATE TABLE IF NOT EXISTS `commercial_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `operator_type` VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT '操作人类型: USER/ADMIN/SYSTEM',
  `company_id` BIGINT DEFAULT NULL COMMENT '企业ID',
  `action` VARCHAR(64) NOT NULL COMMENT '操作: ORDER_CREATE/PAYMENT_CONFIRM/SUBSCRIPTION_ACTIVATE等',
  `entity_type` VARCHAR(32) NOT NULL COMMENT '实体类型',
  `entity_id` BIGINT DEFAULT NULL COMMENT '实体ID',
  `detail` TEXT COMMENT '详情JSON',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_entity` (`entity_type`, `entity_id`),
  KEY `idx_audit_company` (`company_id`),
  KEY `idx_audit_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商业审计日志';

-- ============================================================
-- 初始数据：默认套餐 + 默认广告位
-- ============================================================

-- 默认套餐（价格全0，不自行设置真实收费价格）
INSERT INTO `enterprise_plan` (`code`, `name`, `description`, `level`, `display_price`, `duration_days`, `sort`, `status`, `current_version`, `saleable`)
SELECT 'FREE', '免费版', '基础企业能力', 0, 0.00, 36500, 0, 'ON_SHELF', 1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `enterprise_plan` WHERE `code`='FREE' AND `deleted`=0);

INSERT INTO `enterprise_plan` (`code`, `name`, `description`, `level`, `display_price`, `duration_days`, `sort`, `status`, `current_version`, `saleable`)
SELECT 'BASIC', '企业基础版', '基础商业能力', 10, 0.00, 365, 10, 'DRAFT', 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `enterprise_plan` WHERE `code`='BASIC' AND `deleted`=0);

INSERT INTO `enterprise_plan` (`code`, `name`, `description`, `level`, `display_price`, `duration_days`, `sort`, `status`, `current_version`, `saleable`)
SELECT 'PRO', '企业专业版', '专业商业能力', 20, 0.00, 365, 20, 'DRAFT', 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `enterprise_plan` WHERE `code`='PRO' AND `deleted`=0);

INSERT INTO `enterprise_plan` (`code`, `name`, `description`, `level`, `display_price`, `duration_days`, `sort`, `status`, `current_version`, `saleable`)
SELECT 'ENTERPRISE', '企业旗舰版', '旗舰商业能力', 30, 0.00, 365, 30, 'DRAFT', 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `enterprise_plan` WHERE `code`='ENTERPRISE' AND `deleted`=0);

-- FREE套餐版本（权益配置）
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
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `advertising_slot` WHERE `slot_code`='HOME_BANNER' AND `deleted`=0);

INSERT INTO `advertising_slot` (`slot_code`, `name`, `page_location`, `size_hint`, `allowed_types`, `status`, `sort`)
SELECT 'ROBOT_LIST', '机器人列表侧栏', '机器人列表页右侧', '300x250', 'IMAGE', 'ACTIVE', 10
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `advertising_slot` WHERE `slot_code`='ROBOT_LIST' AND `deleted`=0);

INSERT INTO `advertising_slot` (`slot_code`, `name`, `page_location`, `size_hint`, `allowed_types`, `status`, `sort`)
SELECT 'ROBOT_DETAIL', '机器人详情底部', '机器人详情页底部', '728x90', 'IMAGE', 'ACTIVE', 20
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `advertising_slot` WHERE `slot_code`='ROBOT_DETAIL' AND `deleted`=0);

INSERT INTO `advertising_slot` (`slot_code`, `name`, `page_location`, `size_hint`, `allowed_types`, `status`, `sort`)
SELECT 'COMPANY_PAGE', '企业页侧栏', '企业详情页右侧', '300x250', 'IMAGE', 'ACTIVE', 30
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `advertising_slot` WHERE `slot_code`='COMPANY_PAGE' AND `deleted`=0);

INSERT INTO `advertising_slot` (`slot_code`, `name`, `page_location`, `size_hint`, `allowed_types`, `status`, `sort`)
SELECT 'ARTICLE_DETAIL', '文章详情底部', '文章详情页底部', '728x90', 'IMAGE', 'ACTIVE', 40
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `advertising_slot` WHERE `slot_code`='ARTICLE_DETAIL' AND `deleted`=0);

INSERT INTO `advertising_slot` (`slot_code`, `name`, `page_location`, `size_hint`, `allowed_types`, `status`, `sort`)
SELECT 'SEARCH_RESULT', '搜索结果顶部', '搜索结果页顶部', '728x90', 'IMAGE', 'ACTIVE', 50
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `advertising_slot` WHERE `slot_code`='SEARCH_RESULT' AND `deleted`=0);

-- Phase14: 商业化运营权限
INSERT INTO `sys_permission` (`permission_code`, `permission_name`, `description`, `create_time`, `update_time`, `deleted`)
SELECT 'commercial:plan:manage', '套餐管理', '商业化套餐管理', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='commercial:plan:manage' AND `deleted`=0);

INSERT INTO `sys_permission` (`permission_code`, `permission_name`, `description`, `create_time`, `update_time`, `deleted`)
SELECT 'commercial:order:manage', '订单管理', '商业化订单管理', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='commercial:order:manage' AND `deleted`=0);

INSERT INTO `sys_permission` (`permission_code`, `permission_name`, `description`, `create_time`, `update_time`, `deleted`)
SELECT 'commercial:refund:manage', '退款管理', '商业化退款管理', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='commercial:refund:manage' AND `deleted`=0);

INSERT INTO `sys_permission` (`permission_code`, `permission_name`, `description`, `create_time`, `update_time`, `deleted`)
SELECT 'commercial:subscription:manage', '订阅管理', '商业化订阅管理', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='commercial:subscription:manage' AND `deleted`=0);

INSERT INTO `sys_permission` (`permission_code`, `permission_name`, `description`, `create_time`, `update_time`, `deleted`)
SELECT 'advertising:slot:manage', '广告位管理', '广告位管理', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='advertising:slot:manage' AND `deleted`=0);

INSERT INTO `sys_permission` (`permission_code`, `permission_name`, `description`, `create_time`, `update_time`, `deleted`)
SELECT 'advertising:campaign:review', '广告审核', '广告投放计划审核', NOW(), NOW(), 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `permission_code`='advertising:campaign:review' AND `deleted`=0);

-- ADMIN角色关联新权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, p.id FROM `sys_permission` p
WHERE p.`deleted`=0 AND p.`permission_code` IN (
  'commercial:plan:manage', 'commercial:order:manage', 'commercial:refund:manage',
  'commercial:subscription:manage', 'advertising:slot:manage', 'advertising:campaign:review'
) AND NOT EXISTS (
  SELECT 1 FROM `sys_role_permission` rp WHERE rp.`role_id`=1 AND rp.`permission_id`=p.id
);

-- 清理临时存储过程
DROP PROCEDURE IF EXISTS `p_add_column`;
DROP PROCEDURE IF EXISTS `p_add_index`;
DROP PROCEDURE IF EXISTS `p_add_unique_index`;