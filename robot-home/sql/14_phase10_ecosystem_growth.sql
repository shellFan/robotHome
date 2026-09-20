-- ============================================================
-- Phase10: Ecosystem Growth & Trust 数据库迁移
-- 从Phase9 DB执行此脚本必须成功
-- 字符集：utf8mb4 / 排序：utf8mb4_general_ci
-- 兼容 MySQL 5.6：无窗口函数/CTE/JSON列/utf8mb4_0900
-- 幂等性：所有ALTER TABLE ADD COLUMN/ADD INDEX均带IF NOT EXISTS保护
-- ============================================================

USE robot_home;

-- ============================================================
-- 幂等性辅助：添加列/索引的存储过程
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
-- P0-1 Robot Data Trust / 数据可信度
-- ============================================================

-- 1a. robot_data_source: 机器人数据来源
CREATE TABLE IF NOT EXISTS `robot_data_source` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `robot_id` BIGINT NOT NULL COMMENT '机器人ID',
  `source_type` VARCHAR(32) NOT NULL COMMENT '来源类型: OFFICIAL/VENDOR_SITE/TRUSTED_MEDIA/USER_CORRECTION/CRAWLER/MANUAL',
  `source_name` VARCHAR(255) NOT NULL COMMENT '来源名称(对外展示)',
  `source_url` VARCHAR(1024) DEFAULT NULL COMMENT '来源URL(对外展示)',
  `trust_weight` TINYINT NOT NULL DEFAULT 5 COMMENT '可信权重1-10: OFFICIAL=10,VENDOR=8,MEDIA=6,CRAWLER=3,USER=4,MANUAL=7',
  `verified` TINYINT NOT NULL DEFAULT 0 COMMENT '0未验证 1已验证',
  `verified_time` DATETIME DEFAULT NULL COMMENT '验证时间',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_robot_id` (`robot_id`),
  KEY `idx_source_type` (`source_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人数据来源';

-- 1b. robot表增加trust字段
CALL `p_add_column`('robot', 'trust_level', 'VARCHAR(16) DEFAULT ''NORMAL'' COMMENT ''信任等级: VERIFIED/HIGH/NORMAL/LOW'' AFTER `follow_count`');
CALL `p_add_column`('robot', 'trust_score', 'INT NOT NULL DEFAULT 0 COMMENT ''信任分(0-100)内部使用'' AFTER `trust_level`');
CALL `p_add_column`('robot', 'pending_correction_count', 'INT NOT NULL DEFAULT 0 COMMENT ''待处理纠错数'' AFTER `trust_score`');

CALL `p_add_index`('robot', 'idx_trust_level', '`trust_level`');
CALL `p_add_index`('robot', 'idx_trust_score', '`trust_score`');

-- ============================================================
-- P0-2 Change History / 参数与价格变更历史
-- ============================================================

-- 2a. robot_change_record: 机器人变更记录(公开可见)
CREATE TABLE IF NOT EXISTS `robot_change_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `robot_id` BIGINT NOT NULL COMMENT '机器人ID',
  `change_type` VARCHAR(32) NOT NULL COMMENT '变更类型: PRICE/PARAMETER/VERSION/STATUS/BASIC_INFO',
  `field_name` VARCHAR(64) NOT NULL COMMENT '字段名(公开白名单)',
  `field_label` VARCHAR(128) DEFAULT NULL COMMENT '字段显示名',
  `old_value` TEXT COMMENT '旧值',
  `new_value` TEXT COMMENT '新值',
  `source_type` VARCHAR(32) DEFAULT NULL COMMENT '来源类型: OFFICIAL/CRAWLER/CORRECTION/MANUAL',
  `source_name` VARCHAR(255) DEFAULT NULL COMMENT '来源名称',
  `verified` TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核 1已审核',
  `event_key` CHAR(64) DEFAULT NULL COMMENT '事件唯一键(SHA-256)防重复',
  `change_time` DATETIME NOT NULL COMMENT '变更时间',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_key` (`event_key`),
  KEY `idx_robot_change_type` (`robot_id`, `change_type`),
  KEY `idx_robot_change_time` (`robot_id`, `change_time`),
  KEY `idx_change_type` (`change_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人变更记录';

-- ============================================================
-- P0-3 Subscription Center / 订阅中心
-- ============================================================

-- 3a. user_subscription: 用户订阅
CREATE TABLE IF NOT EXISTS `user_subscription` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_type` VARCHAR(16) NOT NULL COMMENT '目标类型: ROBOT/BRAND/COMPANY',
  `target_id` BIGINT NOT NULL COMMENT '目标ID',
  `event_types` VARCHAR(255) NOT NULL DEFAULT 'NEW_CONTENT,IMPORTANT_CHANGE' COMMENT '订阅事件类型(逗号分隔)',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_user_enabled` (`user_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订阅';

-- ============================================================
-- P0-4 Notification Center 2.0 / 消息中心升级
-- ============================================================

-- 4a. message表增加字段(升级而非重写)
CALL `p_add_column`('message', 'notification_type', 'VARCHAR(32) DEFAULT ''SYSTEM'' COMMENT ''通知类型: SYSTEM/FOLLOW_UPDATE/PARAM_CHANGE/PRICE_CHANGE/QUESTION_ANSWER/ANSWER_ACCEPTED/REVIEW_INTERACTION/POST_INTERACTION/PROCUREMENT_RESPONSE'' AFTER `type`');
CALL `p_add_column`('message', 'target_type', 'VARCHAR(16) DEFAULT NULL COMMENT ''目标类型: robot/brand/company/post/question/answer/review/procurement'' AFTER `notification_type`');
CALL `p_add_column`('message', 'target_id', 'BIGINT DEFAULT NULL COMMENT ''目标ID'' AFTER `target_type`');
CALL `p_add_column`('message', 'summary', 'VARCHAR(512) DEFAULT NULL COMMENT ''通知摘要'' AFTER `target_id`');
CALL `p_add_column`('message', 'event_key', 'CHAR(64) DEFAULT NULL COMMENT ''事件唯一键防重复'' AFTER `summary`');
CALL `p_add_column`('message', 'deleted', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''0正常 1已删除'' AFTER `is_read`');

CALL `p_add_index`('message', 'idx_user_read_deleted', '`user_id`, `is_read`, `deleted`');
CALL `p_add_index`('message', 'idx_notification_type', '`notification_type`');
CALL `p_add_index`('message', 'idx_event_key', '`event_key`');
CALL `p_add_index`('message', 'idx_target', '`target_type`, `target_id`');

-- Phase10补: message表增加clicked字段(CTR追踪)
CALL `p_add_column`('message', 'clicked', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''0未点击 1已点击'' AFTER `deleted`');
CALL `p_add_index`('message', 'idx_clicked', '`clicked`');

-- ============================================================
-- P0-5 Procurement CRM / 采购线索运营
-- ============================================================

-- 5a. inquiry表增加CRM字段
CALL `p_add_column`('inquiry', 'pipeline_status', 'VARCHAR(32) DEFAULT ''NEW'' COMMENT ''Pipeline状态: NEW/CONTACTED/QUALIFIED/MATCHING/RESPONDED/NEGOTIATING/WON/LOST/CLOSED'' AFTER `lead_reason`');
CALL `p_add_column`('inquiry', 'crm_owner', 'BIGINT DEFAULT NULL COMMENT ''CRM负责人(Admin用户ID)'' AFTER `pipeline_status`');
CALL `p_add_column`('inquiry', 'crm_priority', 'TINYINT DEFAULT 0 COMMENT ''CRM优先级: 0普通 1高 2紧急'' AFTER `crm_owner`');
CALL `p_add_column`('inquiry', 'next_follow_time', 'DATETIME DEFAULT NULL COMMENT ''下次跟进时间'' AFTER `crm_priority`');
CALL `p_add_column`('inquiry', 'last_follow_time', 'DATETIME DEFAULT NULL COMMENT ''最后跟进时间'' AFTER `next_follow_time`');
CALL `p_add_column`('inquiry', 'crm_source', 'VARCHAR(32) DEFAULT NULL COMMENT ''线索来源: WEB/MINIAPP/ADMIN/IMPORT'' AFTER `last_follow_time`');
CALL `p_add_column`('inquiry', 'crm_remark', 'VARCHAR(1024) DEFAULT NULL COMMENT ''CRM备注'' AFTER `crm_source`');

CALL `p_add_index`('inquiry', 'idx_pipeline_status', '`pipeline_status`');
CALL `p_add_index`('inquiry', 'idx_crm_owner', '`crm_owner`');
CALL `p_add_index`('inquiry', 'idx_next_follow_time', '`next_follow_time`');
CALL `p_add_index`('inquiry', 'idx_lead_priority', '`lead_priority`');

-- 5b. procurement_follow_record: 采购线索跟进记录
CREATE TABLE IF NOT EXISTS `procurement_follow_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `procurement_id` BIGINT NOT NULL COMMENT '采购需求ID(inquiry.id)',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名',
  `action` VARCHAR(32) NOT NULL COMMENT '操作: STATUS_CHANGE/FOLLOW_UP/CALL/EMAIL/MEETING/NOTE/ASSIGN',
  `content` VARCHAR(1024) DEFAULT NULL COMMENT '跟进内容',
  `old_status` VARCHAR(32) DEFAULT NULL COMMENT '旧状态',
  `new_status` VARCHAR(32) DEFAULT NULL COMMENT '新状态',
  `next_follow_time` DATETIME DEFAULT NULL COMMENT '下次跟进时间',
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_procurement_id` (`procurement_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购线索跟进记录';

-- ============================================================
-- P0-6 Enterprise Response / 企业响应采购需求
-- ============================================================

-- 6a. company_member: 企业成员关系（安全修复：企业身份校验）
CREATE TABLE IF NOT EXISTS `company_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `company_id` BIGINT NOT NULL COMMENT '企业ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role` VARCHAR(16) NOT NULL DEFAULT 'MEMBER' COMMENT '角色: OWNER/ADMIN/MEMBER',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1正常',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_user` (`company_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业成员关系';

-- 6b. procurement_response: 企业响应
CREATE TABLE IF NOT EXISTS `procurement_response` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `procurement_id` BIGINT NOT NULL COMMENT '采购需求ID(inquiry.id)',
  `company_id` BIGINT NOT NULL COMMENT '响应企业ID',
  `contact_user_id` BIGINT NOT NULL COMMENT '联系人用户ID',
  `solution` TEXT COMMENT '解决方案描述',
  `robot_ids` VARCHAR(512) DEFAULT NULL COMMENT '推荐机器人ID(逗号分隔)',
  `price_description` VARCHAR(1024) DEFAULT NULL COMMENT '价格说明',
  `delivery_description` VARCHAR(512) DEFAULT NULL COMMENT '交付说明',
  `contact_description` VARCHAR(512) DEFAULT NULL COMMENT '联系方式说明',
  `status` VARCHAR(16) NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态: SUBMITTED/VIEWED/CONTACTED/ACCEPTED/REJECTED/WITHDRAWN',
  `event_key` CHAR(64) DEFAULT NULL COMMENT '幂等键',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_procurement_company` (`procurement_id`, `company_id`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_contact_user_id` (`contact_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_event_key` (`event_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业采购响应';

-- ============================================================
-- P0-7 Contribution Reputation / 用户贡献信誉
-- ============================================================

-- 7a. user_reputation: 用户信誉
CREATE TABLE IF NOT EXISTS `user_reputation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `reputation_score` INT NOT NULL DEFAULT 0 COMMENT '信誉分',
  `reputation_level` VARCHAR(16) NOT NULL DEFAULT 'NEW' COMMENT '信誉等级: NEW/CONTRIBUTOR/ACTIVE/TRUSTED/EXPERT',
  `accepted_corrections` INT NOT NULL DEFAULT 0 COMMENT '被采纳纠错数',
  `accepted_answers` INT NOT NULL DEFAULT 0 COMMENT '被采纳回答数',
  `helpful_reviews` INT NOT NULL DEFAULT 0 COMMENT '有帮助评价数',
  `quality_posts` INT NOT NULL DEFAULT 0 COMMENT '高质量帖子数',
  `penalty_score` INT NOT NULL DEFAULT 0 COMMENT '惩罚分(负值影响)',
  `daily_contrib_cap` INT NOT NULL DEFAULT 50 COMMENT '每日贡献上限',
  `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_reputation_level` (`reputation_level`),
  KEY `idx_reputation_score` (`reputation_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信誉';

-- 7b. reputation_event: 信誉事件记录
CREATE TABLE IF NOT EXISTS `reputation_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `event_type` VARCHAR(32) NOT NULL COMMENT '事件类型: CORRECTION_ACCEPTED/ANSWER_ACCEPTED/REVIEW_HELPFUL/POST_QUALITY/QUESTION_ANSWERED/ABUSE_PENALTY/SPAM_REJECTED',
  `event_key` CHAR(64) DEFAULT NULL COMMENT '事件唯一键(幂等)',
  `score_delta` INT NOT NULL DEFAULT 0 COMMENT '信誉分变化',
  `reference_type` VARCHAR(16) DEFAULT NULL COMMENT '关联类型: correction/answer/review/post/question',
  `reference_id` BIGINT DEFAULT NULL COMMENT '关联ID',
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_key` (`event_key`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信誉事件记录';

-- 7c. user表增加信誉字段
CALL `p_add_column`('user', 'reputation_score', 'INT NOT NULL DEFAULT 0 COMMENT ''信誉分'' AFTER `answer_count`');
CALL `p_add_column`('user', 'reputation_level', 'VARCHAR(16) DEFAULT ''NEW'' COMMENT ''信誉等级: NEW/CONTRIBUTOR/ACTIVE/TRUSTED/EXPERT'' AFTER `reputation_score`');

CALL `p_add_index`('user', 'idx_reputation_score', '`reputation_score`');

-- ============================================================
-- P0-8 Ecosystem Analytics / 生态增长分析
-- ============================================================

-- 8a. growth_daily_stat增加新指标
CALL `p_add_column`('growth_daily_stat', 'returning_users', 'INT NOT NULL DEFAULT 0 COMMENT ''回访用户数'' AFTER `follows`');
CALL `p_add_column`('growth_daily_stat', 'subscription_count', 'INT NOT NULL DEFAULT 0 COMMENT ''新增订阅数'' AFTER `returning_users`');
CALL `p_add_column`('growth_daily_stat', 'notification_sent', 'INT NOT NULL DEFAULT 0 COMMENT ''通知发送数'' AFTER `subscription_count`');
CALL `p_add_column`('growth_daily_stat', 'notification_clicked', 'INT NOT NULL DEFAULT 0 COMMENT ''通知点击数'' AFTER `notification_sent`');
CALL `p_add_column`('growth_daily_stat', 'contribution_users', 'INT NOT NULL DEFAULT 0 COMMENT ''贡献用户数'' AFTER `notification_clicked`');
CALL `p_add_column`('growth_daily_stat', 'accepted_corrections', 'INT NOT NULL DEFAULT 0 COMMENT ''被采纳纠错数'' AFTER `contribution_users`');
CALL `p_add_column`('growth_daily_stat', 'procurement_leads', 'INT NOT NULL DEFAULT 0 COMMENT ''采购线索数'' AFTER `accepted_corrections`');
CALL `p_add_column`('growth_daily_stat', 'enterprise_responses', 'INT NOT NULL DEFAULT 0 COMMENT ''企业响应数'' AFTER `procurement_leads`');
CALL `p_add_column`('growth_daily_stat', 'follow_conversions', 'INT NOT NULL DEFAULT 0 COMMENT ''关注转化数'' AFTER `enterprise_responses`');

-- ============================================================
-- P1-4 Collections / 机器人清单
-- ============================================================

-- 9a. user_collection: 用户清单
CREATE TABLE IF NOT EXISTS `user_collection` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `name` VARCHAR(128) NOT NULL COMMENT '清单名称',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `visibility` VARCHAR(16) NOT NULL DEFAULT 'PRIVATE' COMMENT '可见性: PRIVATE/PUBLIC',
  `robot_count` INT NOT NULL DEFAULT 0 COMMENT '机器人数',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_visibility` (`visibility`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户机器人清单';

-- 9b. user_collection_item: 清单项
CREATE TABLE IF NOT EXISTS `user_collection_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `collection_id` BIGINT NOT NULL COMMENT '清单ID',
  `robot_id` BIGINT NOT NULL COMMENT '机器人ID',
  `note` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collection_robot` (`collection_id`, `robot_id`),
  KEY `idx_robot_id` (`robot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='清单项';

-- ============================================================
-- P1-5 Content Topic / 内容话题
-- ============================================================

-- 10a. topic: 话题
CREATE TABLE IF NOT EXISTS `topic` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL COMMENT '话题名称',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `icon` VARCHAR(255) DEFAULT NULL COMMENT '图标',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
  `post_count` INT NOT NULL DEFAULT 0 COMMENT '帖子数',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='话题';

-- 10b. topic关联用community_post增加topic_id
CALL `p_add_column`('community_post', 'topic_id', 'BIGINT DEFAULT NULL COMMENT ''话题ID'' AFTER `company_id`');
CALL `p_add_index`('community_post', 'idx_topic_id', '`topic_id`');

-- ============================================================
-- 索引审计补充
-- ============================================================

CALL `p_add_index`('robot_data_source', 'idx_robot_verified', '`robot_id`, `verified`');
CALL `p_add_index`('robot_change_record', 'idx_robot_verified', '`robot_id`, `verified`');
CALL `p_add_index`('procurement_response', 'idx_procurement_status', '`procurement_id`, `status`');
CALL `p_add_index`('reputation_event', 'idx_user_event_type', '`user_id`, `event_type`');

-- ============================================================
-- 清理辅助存储过程
-- ============================================================

DROP PROCEDURE IF EXISTS `p_add_column`;
DROP PROCEDURE IF EXISTS `p_add_index`;