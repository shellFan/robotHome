-- ============================================================
-- Phase7 Content Community Growth 数据库迁移
-- P0-1: Robot Review (口碑评价)
-- P0-2: Parameter Correction (参数纠错)
-- P0-3: Procurement V2 (采购询价扩展)
-- P0-4: Similar Robot (相似推荐)
-- 字符集：utf8mb4 / 排序：utf8mb4_general_ci
-- 兼容 MySQL 5.6：无窗口函数/CTE/JSON列/utf8mb4_0900
-- ============================================================

USE robot_home;

-- ============================================================
-- P0-1: Robot Review 口碑评价
-- ============================================================

-- 评价主表
DROP TABLE IF EXISTS `robot_review`;
CREATE TABLE `robot_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `robot_id` BIGINT NOT NULL COMMENT '机器人ID',
  `user_id` BIGINT NOT NULL COMMENT '评价用户ID',
  `overall_score` TINYINT NOT NULL COMMENT '总体评分 1-5',
  `quality_score` TINYINT NOT NULL COMMENT '质量评分 1-5',
  `service_score` TINYINT NOT NULL COMMENT '服务评分 1-5',
  `cost_score` TINYINT NOT NULL COMMENT '性价比评分 1-5',
  `content` VARCHAR(2000) NOT NULL COMMENT '评价内容',
  `images` VARCHAR(1024) DEFAULT NULL COMMENT '图片URL列表，逗号分隔',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待审核 1已通过 2已拒绝',
  `helpful_count` INT NOT NULL DEFAULT 0 COMMENT '有用投票数',
  `reply_content` VARCHAR(500) DEFAULT NULL COMMENT '官方回复',
  `reply_time` DATETIME DEFAULT NULL COMMENT '官方回复时间',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_robot_id` (`robot_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  UNIQUE KEY `uk_robot_user` (`robot_id`, `user_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人口碑评价';

-- 评价汇总表（每个robot一条记录，定时更新）
DROP TABLE IF EXISTS `robot_review_summary`;
CREATE TABLE `robot_review_summary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `robot_id` BIGINT NOT NULL COMMENT '机器人ID',
  `review_count` INT NOT NULL DEFAULT 0 COMMENT '评价总数',
  `overall_avg` DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '总体平均分',
  `quality_avg` DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '质量平均分',
  `service_avg` DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '服务平均分',
  `cost_avg` DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '性价比平均分',
  `score_5_count` INT NOT NULL DEFAULT 0 COMMENT '5星数量',
  `score_4_count` INT NOT NULL DEFAULT 0 COMMENT '4星数量',
  `score_3_count` INT NOT NULL DEFAULT 0 COMMENT '3星数量',
  `score_2_count` INT NOT NULL DEFAULT 0 COMMENT '2星数量',
  `score_1_count` INT NOT NULL DEFAULT 0 COMMENT '1星数量',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_robot_id` (`robot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价汇总';

-- 有用投票表
DROP TABLE IF EXISTS `robot_review_helpful`;
CREATE TABLE `robot_review_helpful` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `review_id` BIGINT NOT NULL COMMENT '评价ID',
  `user_id` BIGINT NOT NULL COMMENT '投票用户ID',
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_user` (`review_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价有用投票';

-- ============================================================
-- P0-2: Parameter Correction 参数纠错
-- ============================================================

DROP TABLE IF EXISTS `robot_param_correction`;
CREATE TABLE `robot_param_correction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `robot_id` BIGINT NOT NULL COMMENT '机器人ID',
  `user_id` BIGINT NOT NULL COMMENT '提交用户ID',
  `def_id` BIGINT NOT NULL COMMENT '参数定义ID',
  `param_name` VARCHAR(128) DEFAULT NULL COMMENT '参数名称（冗余）',
  `old_value` VARCHAR(500) DEFAULT NULL COMMENT '原值',
  `new_value` VARCHAR(500) NOT NULL COMMENT '建议新值',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '纠错理由',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待审核 1已采纳 2已拒绝',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `review_note` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_robot_id` (`robot_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数纠错';

-- ============================================================
-- P0-3: Procurement V2 采购询价扩展字段
-- ============================================================

ALTER TABLE `inquiry` ADD COLUMN `inquiry_type` VARCHAR(32) DEFAULT 'GENERAL' COMMENT '询价类型: GENERAL/PURCHASE/LEASE/COOPERATE';
ALTER TABLE `inquiry` ADD COLUMN `procurement_scene` VARCHAR(64) DEFAULT NULL COMMENT '采购场景: INDUSTRIAL/LOGISTICS/MEDICAL/EDUCATION/SERVICE/OTHER';
ALTER TABLE `inquiry` ADD COLUMN `purchase_time` VARCHAR(32) DEFAULT NULL COMMENT '期望采购时间';
ALTER TABLE `inquiry` ADD COLUMN `lead_priority` TINYINT DEFAULT 0 COMMENT '线索优先级: 0普通 1高 2紧急';
ALTER TABLE `inquiry` ADD COLUMN `lead_reason` VARCHAR(256) DEFAULT NULL COMMENT '优先级理由（可解释）';
-- quantity/budget 已存在，仅修改budget类型为DECIMAL
ALTER TABLE `inquiry` MODIFY COLUMN `budget` DECIMAL(12,2) DEFAULT NULL COMMENT '预算金额';

-- ============================================================
-- P0-4: Similar Robot 相似推荐
-- ============================================================

DROP TABLE IF EXISTS `robot_similar_score`;
CREATE TABLE `robot_similar_score` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `robot_id` BIGINT NOT NULL COMMENT '源机器人ID',
  `similar_robot_id` BIGINT NOT NULL COMMENT '相似机器人ID',
  `category_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '分类相似分',
  `price_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '价格相似分',
  `brand_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '品牌相似分',
  `param_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '参数相似分',
  `tag_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '标签相似分',
  `total_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '总分',
  `reason` VARCHAR(256) DEFAULT NULL COMMENT '推荐理由',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pair` (`robot_id`, `similar_robot_id`),
  KEY `idx_robot_score` (`robot_id`, `total_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='相似机器人评分';