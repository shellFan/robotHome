-- ============================================================
-- 机器人之家 - 数据采集系统 Schema
-- 字符集：utf8mb4 / 排序：utf8mb4_general_ci
-- ============================================================
USE robot_home;

-- ---------------------------
-- 采集数据源配置
-- ---------------------------
DROP TABLE IF EXISTS `crawler_source`;
CREATE TABLE `crawler_source` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_name` VARCHAR(128) NOT NULL COMMENT '数据源名称',
  `source_type` VARCHAR(32) NOT NULL COMMENT 'WEBSITE/RSS/SITEMAP/WECHAT/MANUAL',
  `base_url` VARCHAR(512) NOT NULL COMMENT '基础URL',
  `brand_id` BIGINT DEFAULT NULL COMMENT '关联品牌',
  `company_id` BIGINT DEFAULT NULL COMMENT '关联企业',
  `crawl_enabled` TINYINT DEFAULT 1 COMMENT '0停用 1启用',
  `crawl_strategy` VARCHAR(32) DEFAULT 'GENERIC' COMMENT 'GENERIC/SITEMAP/RSS/NEWS/PRODUCT/WECHAT',
  `crawl_interval` INT DEFAULT 3600 COMMENT '采集间隔(秒)',
  `max_depth` INT DEFAULT 3 COMMENT '最大爬取深度',
  `max_pages` INT DEFAULT 500 COMMENT '单次最大页面数',
  `include_rules` VARCHAR(1024) DEFAULT NULL COMMENT '包含URL规则(正则,逗号分隔)',
  `exclude_rules` VARCHAR(1024) DEFAULT NULL COMMENT '排除URL规则(正则,逗号分隔)',
  `title_selector` VARCHAR(255) DEFAULT NULL COMMENT '标题CSS选择器',
  `content_selector` VARCHAR(255) DEFAULT NULL COMMENT '正文CSS选择器',
  `date_selector` VARCHAR(255) DEFAULT NULL COMMENT '日期CSS选择器',
  `author_selector` VARCHAR(255) DEFAULT NULL COMMENT '作者CSS选择器',
  `cover_selector` VARCHAR(255) DEFAULT NULL COMMENT '封面CSS选择器',
  `list_selector` VARCHAR(255) DEFAULT NULL COMMENT '列表页CSS选择器',
  `detail_link_selector` VARCHAR(255) DEFAULT NULL COMMENT '详情链接CSS选择器',
  `auto_publish` TINYINT DEFAULT 0 COMMENT '0需审核 1自动发布',
  `respect_robots` TINYINT DEFAULT 1 COMMENT '是否遵守robots.txt',
  `request_delay` INT DEFAULT 1000 COMMENT '请求间隔(毫秒)',
  `user_agent` VARCHAR(255) DEFAULT NULL COMMENT '自定义User-Agent',
  `extra_config` TEXT COMMENT '额外配置JSON',
  `last_crawl_time` DATETIME DEFAULT NULL COMMENT '最后采集时间',
  `last_crawl_status` VARCHAR(32) DEFAULT NULL COMMENT '最后采集状态',
  `total_articles` INT DEFAULT 0 COMMENT '累计文章数',
  `total_products` INT DEFAULT 0 COMMENT '累计产品数',
  `total_errors` INT DEFAULT 0 COMMENT '累计错误数',
  `status` TINYINT DEFAULT 1 COMMENT '0禁用 1正常',
  `seed_urls` TEXT COMMENT '种子URL列表(换行分隔)',
  `sitemap_url` VARCHAR(512) DEFAULT NULL COMMENT 'Sitemap URL',
  `fetch_mode` VARCHAR(16) DEFAULT 'http' COMMENT '抓取模式: http/browser',
  `follow_external` TINYINT DEFAULT 0 COMMENT '是否跟踪外部链接',
  `wait_selector` VARCHAR(255) DEFAULT NULL COMMENT '等待选择器(browser模式)',
  `wait_after_load_ms` INT DEFAULT 2000 COMMENT '页面加载后等待时间(browser模式,毫秒)',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`source_type`),
  KEY `idx_enabled` (`crawl_enabled`),
  KEY `idx_brand` (`brand_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集数据源';

-- ---------------------------
-- 采集任务
-- ---------------------------
DROP TABLE IF EXISTS `crawler_task`;
CREATE TABLE `crawler_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT NOT NULL COMMENT '数据源ID',
  `task_type` VARCHAR(32) NOT NULL COMMENT 'FULL/INCREMENTAL/SINGLE_URL/RETRY',
  `status` VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED/STOPPED',
  `start_time` DATETIME DEFAULT NULL,
  `end_time` DATETIME DEFAULT NULL,
  `urls_discovered` INT DEFAULT 0 COMMENT '发现URL数',
  `urls_success` INT DEFAULT 0 COMMENT '成功URL数',
  `urls_failed` INT DEFAULT 0 COMMENT '失败URL数',
  `articles_new` INT DEFAULT 0 COMMENT '新增文章数',
  `articles_updated` INT DEFAULT 0 COMMENT '更新文章数',
  `articles_duplicate` INT DEFAULT 0 COMMENT '重复文章数',
  `products_new` INT DEFAULT 0 COMMENT '新增产品数',
  `products_updated` INT DEFAULT 0 COMMENT '更新产品数',
  `images_downloaded` INT DEFAULT 0 COMMENT '下载图片数',
  `images_failed` INT DEFAULT 0 COMMENT '失败图片数',
  `error_message` VARCHAR(1024) DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_id`),
  KEY `idx_status` (`status`),
  KEY `idx_start` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集任务';

-- ---------------------------
-- URL队列
-- ---------------------------
DROP TABLE IF EXISTS `crawler_url`;
CREATE TABLE `crawler_url` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT NOT NULL COMMENT '数据源ID',
  `task_id` BIGINT DEFAULT NULL COMMENT '任务ID',
  `url` VARCHAR(1024) NOT NULL COMMENT '原始URL',
  `normalized_url` VARCHAR(1024) NOT NULL COMMENT '规范化URL',
  `url_hash` VARCHAR(64) NOT NULL COMMENT 'URL SHA-256',
  `parent_url` VARCHAR(1024) DEFAULT NULL COMMENT '父页面URL',
  `depth` INT DEFAULT 0 COMMENT '爬取深度',
  `content_type` VARCHAR(32) DEFAULT NULL COMMENT 'HTML/RSS/IMAGE/JSON',
  `url_status` VARCHAR(32) DEFAULT 'DISCOVERED' COMMENT 'DISCOVERED/QUEUED/FETCHING/SUCCESS/FAILED/SKIPPED/BLOCKED',
  `http_status` INT DEFAULT NULL COMMENT 'HTTP状态码',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  `last_error` VARCHAR(512) DEFAULT NULL COMMENT '最后错误',
  `etag` VARCHAR(255) DEFAULT NULL COMMENT 'HTTP ETag',
  `last_modified` VARCHAR(64) DEFAULT NULL COMMENT 'HTTP Last-Modified',
  `crawl_time` DATETIME DEFAULT NULL COMMENT '抓取时间',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_url_hash` (`url_hash`),
  KEY `idx_source` (`source_id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_status` (`url_status`),
  KEY `idx_crawl` (`crawl_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='URL队列';

-- ---------------------------
-- 页面原始内容
-- ---------------------------
DROP TABLE IF EXISTS `crawler_page`;
CREATE TABLE `crawler_page` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `url_id` BIGINT NOT NULL COMMENT 'URL ID',
  `source_id` BIGINT NOT NULL COMMENT '数据源ID',
  `task_id` BIGINT DEFAULT NULL COMMENT '任务ID',
  `url` VARCHAR(1024) NOT NULL,
  `title` VARCHAR(512) DEFAULT NULL COMMENT '页面标题',
  `html_content` MEDIUMTEXT COMMENT '原始HTML',
  `text_content` MEDIUMTEXT COMMENT '提取的文本',
  `content_hash` VARCHAR(64) DEFAULT NULL COMMENT '内容SHA-256',
  `simhash` VARCHAR(64) DEFAULT NULL COMMENT 'SimHash指纹',
  `page_type` VARCHAR(32) DEFAULT NULL COMMENT 'ARTICLE/PRODUCT/LIST/OTHER',
  `links_count` INT DEFAULT 0 COMMENT '页面链接数',
  `images_count` INT DEFAULT 0 COMMENT '页面图片数',
  `crawl_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_url_id` (`url_id`),
  KEY `idx_source` (`source_id`),
  KEY `idx_hash` (`content_hash`),
  KEY `idx_type` (`page_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页面原始内容';

-- ---------------------------
-- 采集文章
-- ---------------------------
DROP TABLE IF EXISTS `crawler_article`;
CREATE TABLE `crawler_article` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT NOT NULL COMMENT '数据源ID',
  `task_id` BIGINT DEFAULT NULL COMMENT '任务ID',
  `page_id` BIGINT DEFAULT NULL COMMENT '页面ID',
  `source_name` VARCHAR(128) DEFAULT NULL COMMENT '来源名称',
  `source_url` VARCHAR(1024) NOT NULL COMMENT '来源URL',
  `source_site` VARCHAR(255) DEFAULT NULL COMMENT '来源站点',
  `title` VARCHAR(512) DEFAULT NULL COMMENT '标题',
  `subtitle` VARCHAR(512) DEFAULT NULL COMMENT '副标题',
  `author` VARCHAR(64) DEFAULT NULL COMMENT '作者',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `cover_image` VARCHAR(512) DEFAULT NULL COMMENT '封面图(原始URL)',
  `cover_image_local` VARCHAR(512) DEFAULT NULL COMMENT '封面图(本地URL)',
  `summary` VARCHAR(1024) DEFAULT NULL COMMENT '摘要',
  `content_html` MEDIUMTEXT COMMENT '正文HTML',
  `content_text` MEDIUMTEXT COMMENT '正文纯文本',
  `images` TEXT COMMENT '图片列表JSON',
  `tags` VARCHAR(512) DEFAULT NULL COMMENT '标签JSON',
  `category` VARCHAR(64) DEFAULT NULL COMMENT '分类',
  `content_hash` VARCHAR(64) DEFAULT NULL COMMENT '内容哈希',
  `simhash` VARCHAR(64) DEFAULT NULL COMMENT 'SimHash',
  `brand_id` BIGINT DEFAULT NULL COMMENT '匹配品牌ID',
  `category_id` BIGINT DEFAULT NULL COMMENT '匹配分类ID',
  `series_id` BIGINT DEFAULT NULL COMMENT '匹配系列ID',
  `company_id` BIGINT DEFAULT NULL COMMENT '匹配企业ID',
  `robot_id` BIGINT DEFAULT NULL COMMENT '匹配机器人ID',
  `match_status` VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING/MATCHED/UNMATCHED',
  `article_status` VARCHAR(32) DEFAULT 'CRAWLED' COMMENT 'CRAWLED/PARSED/PENDING_REVIEW/AUTO_APPROVED/PUBLISHED/REJECTED/DUPLICATE/FAILED',
  `article_id` BIGINT DEFAULT NULL COMMENT '同步到article表后的ID',
  `synced` TINYINT DEFAULT 0 COMMENT '0未同步 1已同步',
  `fail_reason` VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  `next_retry_time` DATETIME DEFAULT NULL COMMENT '下次重试时间（指数退避）',
  `crawl_time` DATETIME DEFAULT NULL COMMENT '抓取时间',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_hash` (`content_hash`),
  KEY `idx_status` (`article_status`),
  KEY `idx_match` (`match_status`),
  KEY `idx_sync` (`synced`),
  KEY `idx_publish` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集文章';

-- ---------------------------
-- 采集产品
-- ---------------------------
DROP TABLE IF EXISTS `crawler_product`;
CREATE TABLE `crawler_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT NOT NULL COMMENT '数据源ID',
  `task_id` BIGINT DEFAULT NULL COMMENT '任务ID',
  `page_id` BIGINT DEFAULT NULL COMMENT '页面ID',
  `source_name` VARCHAR(128) DEFAULT NULL COMMENT '来源名称',
  `source_url` VARCHAR(1024) NOT NULL COMMENT '来源URL',
  `source_site` VARCHAR(255) DEFAULT NULL COMMENT '来源站点',
  `product_name` VARCHAR(255) NOT NULL COMMENT '产品名称',
  `model` VARCHAR(128) DEFAULT NULL COMMENT '型号',
  `brand_name` VARCHAR(128) DEFAULT NULL COMMENT '品牌名称(原始)',
  `category` VARCHAR(64) DEFAULT NULL COMMENT '分类',
  `summary` VARCHAR(1024) DEFAULT NULL COMMENT '摘要',
  `description` MEDIUMTEXT COMMENT '描述HTML',
  `cover_image` VARCHAR(512) DEFAULT NULL COMMENT '封面图(原始)',
  `cover_image_local` VARCHAR(512) DEFAULT NULL COMMENT '封面图(本地)',
  `gallery` TEXT COMMENT '图集JSON(原始URL)',
  `gallery_local` TEXT COMMENT '图集JSON(本地URL)',
  `status` VARCHAR(32) DEFAULT NULL COMMENT '在售/预售/停产',
  `release_date` DATE DEFAULT NULL COMMENT '发布日期',
  `price` VARCHAR(128) DEFAULT NULL COMMENT '价格(原始)',
  `official_url` VARCHAR(512) DEFAULT NULL COMMENT '官方链接',
  `raw_params` TEXT COMMENT '原始参数JSON',
  `normalized_params` TEXT COMMENT '标准化参数JSON',
  `content_hash` VARCHAR(64) DEFAULT NULL COMMENT '内容哈希',
  `brand_id` BIGINT DEFAULT NULL COMMENT '匹配品牌ID',
  `category_id` BIGINT DEFAULT NULL COMMENT '匹配分类ID',
  `series_id` BIGINT DEFAULT NULL COMMENT '匹配系列ID',
  `company_id` BIGINT DEFAULT NULL COMMENT '匹配企业ID',
  `robot_id` BIGINT DEFAULT NULL COMMENT '匹配机器人ID',
  `match_status` VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING/MATCHED/UNMATCHED',
  `product_status` VARCHAR(32) DEFAULT 'CRAWLED' COMMENT 'CRAWLED/PARSED/PENDING_REVIEW/AUTO_APPROVED/PUBLISHED/REJECTED/DUPLICATE/FAILED',
  `robot_id_synced` BIGINT DEFAULT NULL COMMENT '同步到robot表后的ID',
  `synced` TINYINT DEFAULT 0 COMMENT '0未同步 1已同步',
  `fail_reason` VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  `next_retry_time` DATETIME DEFAULT NULL COMMENT '下次重试时间（指数退避）',
  `crawl_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_hash` (`content_hash`),
  KEY `idx_status` (`product_status`),
  KEY `idx_match` (`match_status`),
  KEY `idx_sync` (`synced`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集产品';

-- ---------------------------
-- 采集媒体(图片)
-- ---------------------------
DROP TABLE IF EXISTS `crawler_media`;
CREATE TABLE `crawler_media` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT DEFAULT NULL COMMENT '数据源ID',
  `original_url` VARCHAR(1024) NOT NULL COMMENT '原始URL',
  `url_hash` VARCHAR(64) NOT NULL COMMENT 'URL哈希',
  `storage_url` VARCHAR(512) DEFAULT NULL COMMENT '本地存储URL',
  `file_hash` VARCHAR(64) DEFAULT NULL COMMENT '文件内容哈希',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小',
  `mime_type` VARCHAR(64) DEFAULT NULL COMMENT 'MIME类型',
  `width` INT DEFAULT NULL COMMENT '宽度',
  `height` INT DEFAULT NULL COMMENT '高度',
  `media_type` VARCHAR(16) DEFAULT 'IMAGE' COMMENT 'IMAGE/VIDEO',
  `download_status` VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED',
  `retry_count` INT DEFAULT 0,
  `error_message` VARCHAR(512) DEFAULT NULL,
  `ref_article_id` BIGINT DEFAULT NULL COMMENT '关联文章ID',
  `ref_product_id` BIGINT DEFAULT NULL COMMENT '关联产品ID',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_url_hash` (`url_hash`),
  KEY `idx_file_hash` (`file_hash`),
  KEY `idx_status` (`download_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集媒体';

-- ---------------------------
-- 采集错误
-- ---------------------------
DROP TABLE IF EXISTS `crawler_error`;
CREATE TABLE `crawler_error` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT DEFAULT NULL COMMENT '数据源ID',
  `task_id` BIGINT DEFAULT NULL COMMENT '任务ID',
  `url` VARCHAR(1024) DEFAULT NULL COMMENT '相关URL',
  `error_type` VARCHAR(32) NOT NULL COMMENT 'NETWORK/TIMEOUT/HTTP_403/HTTP_404/PARSE_ERROR/JS_RENDER_ERROR/DUPLICATE/DATABASE/IMAGE/BLOCKED/UNKNOWN',
  `error_message` VARCHAR(1024) DEFAULT NULL,
  `error_detail` TEXT COMMENT '详细错误信息',
  `http_status` INT DEFAULT NULL,
  `retry_count` INT DEFAULT 0,
  `resolved` TINYINT DEFAULT 0 COMMENT '0未解决 1已解决',
  `resolve_note` VARCHAR(512) DEFAULT NULL COMMENT '解决备注',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_id`),
  KEY `idx_type` (`error_type`),
  KEY `idx_resolved` (`resolved`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集错误';

-- ---------------------------
-- 匹配记录
-- ---------------------------
DROP TABLE IF EXISTS `crawler_match_record`;
CREATE TABLE `crawler_match_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT DEFAULT NULL COMMENT '数据源ID',
  `biz_type` VARCHAR(32) NOT NULL COMMENT 'ARTICLE/PRODUCT',
  `biz_id` BIGINT NOT NULL COMMENT '业务ID(crawler_article/crawler_product)',
  `match_type` VARCHAR(32) NOT NULL COMMENT 'BRAND/COMPANY/ROBOT',
  `match_target_id` BIGINT DEFAULT NULL COMMENT '匹配目标ID',
  `match_keyword` VARCHAR(255) DEFAULT NULL COMMENT '匹配关键词',
  `confidence` DECIMAL(5,4) DEFAULT 0 COMMENT '匹配置信度',
  `match_status` VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING/AUTO_MATCHED/MANUAL_MATCHED/REJECTED',
  `handled_by` BIGINT DEFAULT NULL COMMENT '处理人',
  `handle_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_match_type` (`match_type`),
  KEY `idx_status` (`match_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='匹配记录';

-- ---------------------------
-- 采集规则
-- ---------------------------
DROP TABLE IF EXISTS `crawler_rule`;
CREATE TABLE `crawler_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT DEFAULT NULL COMMENT '数据源ID(NULL=全局规则)',
  `rule_type` VARCHAR(32) NOT NULL COMMENT 'SELECTOR/REGEX/KEYWORD/PARAM_MAP/CATEGORY',
  `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
  `rule_key` VARCHAR(128) DEFAULT NULL COMMENT '规则键(如title_selector)',
  `rule_value` TEXT COMMENT '规则值(如CSS选择器/正则/JSON)',
  `priority` INT DEFAULT 0 COMMENT '优先级(越大越优先)',
  `enabled` TINYINT DEFAULT 1 COMMENT '0禁用 1启用',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_id`),
  KEY `idx_type` (`rule_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集规则';

-- ---------------------------
-- 采集日志
-- ---------------------------
DROP TABLE IF EXISTS `crawler_log`;
CREATE TABLE `crawler_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `source_id` BIGINT DEFAULT NULL COMMENT '数据源ID',
  `task_id` BIGINT DEFAULT NULL COMMENT '任务ID',
  `url` VARCHAR(1024) DEFAULT NULL COMMENT '相关URL',
  `action` VARCHAR(64) DEFAULT NULL COMMENT '动作',
  `level` VARCHAR(16) DEFAULT 'INFO' COMMENT 'INFO/WARN/ERROR',
  `message` VARCHAR(1024) DEFAULT NULL,
  `duration` INT DEFAULT NULL COMMENT '耗时(毫秒)',
  `http_status` INT DEFAULT NULL,
  `result` VARCHAR(32) DEFAULT NULL COMMENT 'SUCCESS/FAILED/SKIPPED',
  `extra_data` TEXT COMMENT '额外数据JSON',
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_level` (`level`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集日志';

-- ---------------------------
-- 产品参数变更记录
-- ---------------------------
DROP TABLE IF EXISTS `crawler_product_change`;
CREATE TABLE `crawler_product_change` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL COMMENT '采集产品ID',
  `field_name` VARCHAR(64) NOT NULL COMMENT '字段名',
  `old_value` TEXT COMMENT '旧值',
  `new_value` TEXT COMMENT '新值',
  `source_url` VARCHAR(1024) DEFAULT NULL COMMENT '来源URL',
  `change_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品参数变更记录';

-- ---------------------------
-- 品牌别名(用于匹配)
-- ---------------------------
DROP TABLE IF EXISTS `crawler_brand_alias`;
CREATE TABLE `crawler_brand_alias` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `brand_id` BIGINT NOT NULL COMMENT '品牌ID',
  `alias_name` VARCHAR(128) NOT NULL COMMENT '别名',
  `alias_type` VARCHAR(32) DEFAULT 'NAME' COMMENT 'NAME/ENGLISH/ABBREVIATION/OTHER',
  `brand_name` VARCHAR(128) DEFAULT NULL COMMENT '品牌名称(冗余)',
  `is_active` TINYINT DEFAULT 1 COMMENT '0停用 1启用',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_brand_alias` (`brand_id`, `alias_name`),
  KEY `idx_alias` (`alias_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品牌别名';

-- ---------------------------
-- 参数标准化映射
-- ---------------------------
DROP TABLE IF EXISTS `crawler_param_mapping`;
CREATE TABLE `crawler_param_mapping` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `normalized_name` VARCHAR(64) NOT NULL COMMENT '标准化名称',
  `raw_name` VARCHAR(128) NOT NULL COMMENT '原始名称',
  `category` VARCHAR(64) DEFAULT NULL COMMENT '机器人分类',
  `unit` VARCHAR(16) DEFAULT NULL COMMENT '单位',
  `aliases` TEXT COMMENT '别名列表JSON',
  `is_active` TINYINT DEFAULT 1 COMMENT '0停用 1启用',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_raw_name` (`raw_name`, `category`),
  KEY `idx_normalized` (`normalized_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数标准化映射';

-- 初始化参数标准化映射数据
INSERT IGNORE INTO `crawler_param_mapping` (`normalized_name`, `raw_name`, `unit`, `create_time`) VALUES
('weight', '重量', 'kg', NOW()),
('weight', '整机重量', 'kg', NOW()),
('weight', '机器重量', 'kg', NOW()),
('weight', '产品重量', 'kg', NOW()),
('weight', '净重', 'kg', NOW()),
('weight', '自重', 'kg', NOW()),
('height', '身高', 'cm', NOW()),
('height', '产品高度', 'cm', NOW()),
('height', '整机高度', 'cm', NOW()),
('height', '站立高度', 'cm', NOW()),
('dof', '自由度', NULL, NOW()),
('dof', '关节自由度', NULL, NOW()),
('dof', '运动自由度', NULL, NOW()),
('battery_life', '续航', 'h', NOW()),
('battery_life', '续航时间', 'h', NOW()),
('battery_life', '工作时间', 'h', NOW()),
('battery_life', '续航能力', 'h', NOW()),
('speed', '速度', 'm/s', NOW()),
('speed', '最大速度', 'm/s', NOW()),
('speed', '行走速度', 'm/s', NOW()),
('speed', '移动速度', 'm/s', NOW()),
('payload', '负载', 'kg', NOW()),
('payload', '最大负载', 'kg', NOW()),
('payload', '载重', 'kg', NOW()),
('payload', '有效载荷', 'kg', NOW()),
('battery_capacity', '电池容量', 'mAh', NOW()),
('battery_capacity', '电量', 'mAh', NOW()),
('charging_time', '充电时间', 'h', NOW()),
('width', '宽度', 'cm', NOW()),
('width', '产品宽度', 'cm', NOW()),
('length', '长度', 'cm', NOW()),
('length', '产品长度', 'cm', NOW()),
('arm_reach', '臂展', 'cm', NOW()),
('arm_reach', '工作半径', 'cm', NOW()),
('arm_reach', '最大伸展', 'cm', NOW()),
('ip_rating', '防护等级', NULL, NOW()),
('ip_rating', '防水等级', NULL, NOW()),
('operating_temp', '工作温度', '℃', NOW()),
('operating_temp', '使用温度', '℃', NOW()),
('noise_level', '噪音', 'dB', NOW()),
('noise_level', '噪音等级', 'dB', NOW());

-- 初始化品牌别名(基于现有brand表数据)
INSERT IGNORE INTO `crawler_brand_alias` (`brand_id`, `alias_name`, `alias_type`, `create_time`)
SELECT b.id, b.name, 'NAME', NOW() FROM brand b WHERE b.deleted = 0;