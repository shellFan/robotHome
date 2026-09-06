-- ============================================================
-- 机器人之家 Phase 2 迁移：品牌别名 + 参数映射 + 索引优化
-- 兼容 MySQL 5.6
-- ============================================================
USE robot_home;

-- ---------------------------
-- 品牌别名表（用于采集匹配）
-- ---------------------------
CREATE TABLE IF NOT EXISTS `brand_alias` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `brand_id` BIGINT NOT NULL COMMENT '品牌ID',
  `alias_name` VARCHAR(128) NOT NULL COMMENT '别名',
  `alias_type` VARCHAR(32) DEFAULT 'NAME' COMMENT 'NAME/ENGLISH/ABBREVIATION/OTHER',
  `brand_name` VARCHAR(128) DEFAULT NULL COMMENT '品牌名称(冗余)',
  `is_active` TINYINT DEFAULT 1 COMMENT '0停用 1启用',
  `data_source` VARCHAR(16) DEFAULT 'MANUAL' COMMENT 'DEMO/OFFICIAL/CRAWLER/MANUAL',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_brand_alias` (`brand_id`, `alias_name`),
  KEY `idx_alias` (`alias_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品牌别名';

-- ---------------------------
-- 参数标准化映射表
-- ---------------------------
CREATE TABLE IF NOT EXISTS `param_mapping` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `normalized_name` VARCHAR(64) NOT NULL COMMENT '标准化名称',
  `raw_name` VARCHAR(128) NOT NULL COMMENT '原始名称',
  `category` VARCHAR(64) DEFAULT NULL COMMENT '机器人分类',
  `unit` VARCHAR(16) DEFAULT NULL COMMENT '单位',
  `aliases` TEXT COMMENT '别名列表JSON',
  `data_source` VARCHAR(16) DEFAULT 'MANUAL' COMMENT 'DEMO/OFFICIAL/CRAWLER/MANUAL',
  `is_active` TINYINT DEFAULT 1 COMMENT '0停用 1启用',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_raw_name` (`raw_name`, `category`),
  KEY `idx_normalized` (`normalized_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数标准化映射';

-- ---------------------------
-- 产品参数变更记录
-- ---------------------------
CREATE TABLE IF NOT EXISTS `product_change` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `robot_id` BIGINT NOT NULL COMMENT '机器人ID',
  `param_key` VARCHAR(64) NOT NULL COMMENT '参数键',
  `old_value` TEXT COMMENT '旧值',
  `new_value` TEXT COMMENT '新值',
  `source_url` VARCHAR(1024) DEFAULT NULL COMMENT '来源URL',
  `source_name` VARCHAR(128) DEFAULT NULL COMMENT '来源名称',
  `detected_time` DATETIME DEFAULT NULL COMMENT '检测时间',
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_robot` (`robot_id`),
  KEY `idx_param` (`param_key`),
  KEY `idx_detected` (`detected_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品参数变更记录';

-- ---------------------------
-- 索引优化（高频查询）
-- ---------------------------

-- article 表搜索优化
ALTER TABLE `article` ADD INDEX `idx_title` (`title`(191));
ALTER TABLE `article` ADD INDEX `idx_brand` (`brand_id`);
ALTER TABLE `article` ADD INDEX `idx_status_publish` (`status`, `publish_time`);

-- robot 表搜索优化
ALTER TABLE `robot` ADD INDEX `idx_name` (`name`(191));

-- crawler_article 来源URL索引（前缀191兼容utf8mb4）
ALTER TABLE `crawler_article` ADD INDEX `idx_source_url` (`source_url`(191));
ALTER TABLE `crawler_article` ADD INDEX `idx_article_id` (`article_id`);

-- crawler_product 来源URL索引
ALTER TABLE `crawler_product` ADD INDEX `idx_source_url` (`source_url`(191));
ALTER TABLE `crawler_product` ADD INDEX `idx_robot_id_synced` (`robot_id_synced`);

-- comment 表优化
ALTER TABLE `comment` ADD INDEX `idx_create_time` (`create_time`);

-- favorite 表优化
ALTER TABLE `favorite` ADD INDEX `idx_create_time` (`create_time`);

-- search_keyword 表优化（已有uk_keyword，补充时间索引）
ALTER TABLE `search_keyword` ADD INDEX `idx_create_time` (`create_time`);

-- inquiry 表优化
ALTER TABLE `inquiry` ADD INDEX `idx_create_time` (`create_time`);

-- brand 表新增字段（区分正式/demo数据 + 来源追踪 + 英文名）
ALTER TABLE `brand` ADD COLUMN `data_source` VARCHAR(16) DEFAULT 'MANUAL' COMMENT 'DEMO/OFFICIAL/CRAWLER/MANUAL' AFTER `intro`;
ALTER TABLE `brand` ADD COLUMN `brand_name_en` VARCHAR(128) DEFAULT NULL COMMENT '品牌英文名' AFTER `name`;
ALTER TABLE `brand` ADD COLUMN `short_name` VARCHAR(64) DEFAULT NULL COMMENT '简称' AFTER `brand_name_en`;
ALTER TABLE `brand` ADD COLUMN `source_url` VARCHAR(512) DEFAULT NULL COMMENT '来源URL' AFTER `country`;
ALTER TABLE `brand` ADD COLUMN `last_verified_time` DATETIME DEFAULT NULL COMMENT '最后验证时间' AFTER `source_url`;

-- company 表新增字段
ALTER TABLE `company` ADD COLUMN `data_source` VARCHAR(16) DEFAULT 'MANUAL' COMMENT 'DEMO/OFFICIAL/CRAWLER/MANUAL' AFTER `intro`;
ALTER TABLE `company` ADD COLUMN `company_name_en` VARCHAR(128) DEFAULT NULL COMMENT '公司英文名' AFTER `name`;
ALTER TABLE `company` ADD COLUMN `short_name` VARCHAR(64) DEFAULT NULL COMMENT '简称' AFTER `company_name_en`;
ALTER TABLE `company` ADD COLUMN `province` VARCHAR(32) DEFAULT NULL COMMENT '省份' AFTER `region`;
ALTER TABLE `company` ADD COLUMN `city` VARCHAR(32) DEFAULT NULL COMMENT '城市' AFTER `province`;
ALTER TABLE `company` ADD COLUMN `business_scope` VARCHAR(512) DEFAULT NULL COMMENT '经营范围' AFTER `data_source`;
ALTER TABLE `company` ADD COLUMN `source_url` VARCHAR(512) DEFAULT NULL COMMENT '来源URL' AFTER `business_scope`;
ALTER TABLE `company` ADD COLUMN `last_verified_time` DATETIME DEFAULT NULL COMMENT '最后验证时间' AFTER `source_url`;
ALTER TABLE `company` ADD COLUMN `country` VARCHAR(64) DEFAULT NULL COMMENT '国家' AFTER `region`;

-- robot 表新增字段
ALTER TABLE `robot` ADD COLUMN `data_source` VARCHAR(16) DEFAULT 'MANUAL' COMMENT 'DEMO/OFFICIAL/CRAWLER/MANUAL' AFTER `main_params`;
ALTER TABLE `robot` ADD COLUMN `source_url` VARCHAR(512) DEFAULT NULL COMMENT '来源URL' AFTER `data_source`;
ALTER TABLE `robot` ADD COLUMN `source_name` VARCHAR(128) DEFAULT NULL COMMENT '来源名称' AFTER `source_url`;
ALTER TABLE `robot` ADD COLUMN `last_verified_time` DATETIME DEFAULT NULL COMMENT '最后验证时间' AFTER `source_name`;