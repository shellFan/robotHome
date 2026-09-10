-- ============================================================
-- Phase6 Beta 产品化 数据库迁移
-- 字符集：utf8mb4 / 排序：utf8mb4_general_ci
-- 兼容 MySQL 5.6：无窗口函数/CTE/JSON列/utf8mb4_0900
-- ============================================================

USE robot_home;

-- ============================================================
-- 1. 行为事件表（行为追踪 + 热度计算数据源）
-- ============================================================
DROP TABLE IF EXISTS `behavior_event`;
CREATE TABLE `behavior_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（匿名浏览可为空）',
  `session_id` VARCHAR(64) DEFAULT NULL COMMENT '会话ID',
  `event_type` VARCHAR(32) NOT NULL COMMENT 'VIEW/SEARCH/CLICK/FAVORITE/UNFAVORITE/COMPARE/INQUIRY/SHARE/COMMENT/FOLLOW',
  `biz_type` VARCHAR(16) DEFAULT NULL COMMENT 'robot/article/video/post/tutorial/brand/company',
  `biz_id` BIGINT DEFAULT NULL COMMENT '业务对象ID',
  `extra` VARCHAR(512) DEFAULT NULL COMMENT '扩展信息（搜索词/对比IDs等）',
  `ip` VARCHAR(64) DEFAULT NULL,
  `user_agent` VARCHAR(512) DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_event_time` (`event_type`, `create_time`),
  KEY `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行为事件';

-- ============================================================
-- 2. 询价跟进记录表
-- ============================================================
DROP TABLE IF EXISTS `inquiry_follow`;
CREATE TABLE `inquiry_follow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `inquiry_id` BIGINT NOT NULL COMMENT '询价ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作人（后台管理员）',
  `action` VARCHAR(32) NOT NULL COMMENT 'CONTACTED/FOLLOWING/CLOSED/INVALID/NOTE',
  `note` VARCHAR(1024) DEFAULT NULL COMMENT '跟进备注',
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_inquiry` (`inquiry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='询价跟进记录';

-- ============================================================
-- 3. 询价表增加字段
-- ============================================================
ALTER TABLE `inquiry`
  ADD COLUMN `contact_name` VARCHAR(64) DEFAULT NULL COMMENT '联系人姓名' AFTER `phone`,
  ADD COLUMN `email` VARCHAR(128) DEFAULT NULL COMMENT '联系邮箱' AFTER `contact_name`,
  ADD COLUMN `source` VARCHAR(32) DEFAULT 'pc' COMMENT '来源(pc/miniapp)' AFTER `email`,
  ADD COLUMN `assigned_to` BIGINT DEFAULT NULL COMMENT '分配给（后台管理员ID）' AFTER `source`;

-- ============================================================
-- 4. 对比参数定义增强（comparison_type + unit 归一化）
-- ============================================================
ALTER TABLE `robot_param_def`
  ADD COLUMN `comparison_type` VARCHAR(32) DEFAULT 'NEUTRAL' COMMENT 'HIGHER_BETTER/LOWER_BETTER/NEUTRAL/BOOLEAN/TEXT' AFTER `is_compare`,
  ADD COLUMN `unit_group` VARCHAR(32) DEFAULT NULL COMMENT '单位组(kg/g/m/mm/km/h等)用于归一化' AFTER `comparison_type`,
  ADD COLUMN `display_format` VARCHAR(64) DEFAULT NULL COMMENT '显示格式模板，如 {value}{unit}' AFTER `unit_group`;

-- ============================================================
-- 5. 排行榜快照表（Redis ZSET 的 MySQL 持久化）
-- ============================================================
DROP TABLE IF EXISTS `ranking_snapshot`;
CREATE TABLE `ranking_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rank_type` VARCHAR(32) NOT NULL COMMENT 'hot/humanoid/quadruped/service/industrial/family/dev',
  `rank_date` DATE NOT NULL COMMENT '快照日期',
  `robot_id` BIGINT NOT NULL,
  `rank_position` INT NOT NULL COMMENT '排名',
  `score` BIGINT DEFAULT 0 COMMENT '热度分',
  `view_count` INT DEFAULT 0,
  `favorite_count` INT DEFAULT 0,
  `compare_count` INT DEFAULT 0,
  `inquiry_count` INT DEFAULT 0,
  `comment_count` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_date_robot` (`rank_type`, `rank_date`, `robot_id`),
  KEY `idx_type_date` (`rank_type`, `rank_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排行榜快照';

-- ============================================================
-- 6. 排行榜权重配置表（配置化权重，不硬编码）
-- ============================================================
DROP TABLE IF EXISTS `ranking_weight`;
CREATE TABLE `ranking_weight` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `event_type` VARCHAR(32) NOT NULL COMMENT 'VIEW/FAVORITE/COMPARE/INQUIRY/COMMENT/SCORE',
  `weight` INT NOT NULL DEFAULT 0 COMMENT '权重值',
  `description` VARCHAR(128) DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event` (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排行榜权重配置';

-- 初始权重数据
INSERT INTO `ranking_weight` (`event_type`, `weight`, `description`, `create_time`) VALUES
('VIEW', 1, '浏览', NOW()),
('FAVORITE', 8, '收藏', NOW()),
('COMPARE', 12, '对比', NOW()),
('INQUIRY', 30, '询价（强购买意向，权重最高）', NOW()),
('COMMENT', 5, '评论', NOW()),
('SCORE', 10, '评分(0-10)×10', NOW()),
('NEW_PRODUCT', 2, '新品加权：max(0, 365-距发布天数)×2', NOW());

-- ============================================================
-- 7. 时间衰减配置表
-- ============================================================
DROP TABLE IF EXISTS `ranking_decay_config`;
CREATE TABLE `ranking_decay_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rank_type` VARCHAR(32) NOT NULL,
  `half_life_days` INT NOT NULL DEFAULT 90 COMMENT '半衰期（天），热度减半所需天数',
  `min_decay` DECIMAL(5,4) DEFAULT 0.1000 COMMENT '最低衰减系数（防止旧内容永久霸榜）',
  `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type` (`rank_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时间衰减配置';

-- 初始衰减配置
INSERT INTO `ranking_decay_config` (`rank_type`, `half_life_days`, `min_decay`, `update_time`) VALUES
('hot', 90, 0.1000, NOW()),
('humanoid', 120, 0.1000, NOW()),
('quadruped', 120, 0.1000, NOW()),
('service', 90, 0.1000, NOW()),
('industrial', 180, 0.0500, NOW()),
('family', 90, 0.1000, NOW()),
('dev', 60, 0.1500, NOW());

-- ============================================================
-- 8. 限流配置表
-- ============================================================
DROP TABLE IF EXISTS `rate_limit_config`;
CREATE TABLE `rate_limit_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `limit_key` VARCHAR(64) NOT NULL COMMENT '限流标识(login/sms/search/inquiry/comment/post/like/follow/feedback/upload)',
  `max_requests` INT NOT NULL DEFAULT 10 COMMENT '最大请求数',
  `window_seconds` INT NOT NULL DEFAULT 60 COMMENT '时间窗口（秒）',
  `description` VARCHAR(128) DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`limit_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限流配置';

-- 初始限流配置
INSERT INTO `rate_limit_config` (`limit_key`, `max_requests`, `window_seconds`, `description`, `create_time`) VALUES
('login', 5, 300, '登录：5次/5分钟', NOW()),
('sms', 1, 60, '短信验证码：1次/分钟', NOW()),
('search', 30, 60, '搜索：30次/分钟', NOW()),
('inquiry', 3, 3600, '询价：3次/小时', NOW()),
('comment', 10, 60, '评论：10次/分钟', NOW()),
('post', 5, 300, '发帖：5次/5分钟', NOW()),
('like', 30, 60, '点赞：30次/分钟', NOW()),
('follow', 20, 60, '关注：20次/分钟', NOW()),
('feedback', 3, 3600, '反馈：3次/小时', NOW()),
('upload', 10, 60, '上传：10次/分钟', NOW());

-- ============================================================
-- 9. 搜索建议表
-- ============================================================
DROP TABLE IF EXISTS `search_suggestion`;
CREATE TABLE `search_suggestion` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `keyword` VARCHAR(128) NOT NULL,
  `type` VARCHAR(16) DEFAULT 'robot' COMMENT 'robot/brand/article等',
  `weight` INT DEFAULT 0 COMMENT '权重（搜索次数）',
  `status` TINYINT DEFAULT 1,
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_keyword_type` (`keyword`, `type`),
  KEY `idx_weight` (`weight` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索建议';

-- ============================================================
-- 10. 机器人表增加字段（详情页产品化）
-- ============================================================
ALTER TABLE `robot`
  ADD COLUMN `weight` DECIMAL(10,2) DEFAULT NULL COMMENT '重量(kg)' AFTER `main_params`,
  ADD COLUMN `payload` DECIMAL(10,2) DEFAULT NULL COMMENT '负载(kg)' AFTER `weight`,
  ADD COLUMN `max_speed` DECIMAL(10,2) DEFAULT NULL COMMENT '最大速度(m/s)' AFTER `payload`,
  ADD COLUMN `battery_life` DECIMAL(10,2) DEFAULT NULL COMMENT '续航时间(h)' AFTER `max_speed`,
  ADD COLUMN `operating_temp` VARCHAR(64) DEFAULT NULL COMMENT '工作温度范围' AFTER `battery_life`,
  ADD COLUMN `protection_level` VARCHAR(16) DEFAULT NULL COMMENT '防护等级(IP54等)' AFTER `operating_temp`,
  ADD COLUMN `seo_title` VARCHAR(128) DEFAULT NULL COMMENT 'SEO标题' AFTER `protection_level`,
  ADD COLUMN `seo_keywords` VARCHAR(255) DEFAULT NULL COMMENT 'SEO关键词' AFTER `seo_title`,
  ADD COLUMN `seo_description` VARCHAR(512) DEFAULT NULL COMMENT 'SEO描述' AFTER `seo_keywords`;

-- ============================================================
-- 11. 品牌表增加字段（品牌主页产品化）
-- ============================================================
ALTER TABLE `brand`
  ADD COLUMN `seo_title` VARCHAR(128) DEFAULT NULL COMMENT 'SEO标题' AFTER `hot_score`,
  ADD COLUMN `seo_keywords` VARCHAR(255) DEFAULT NULL COMMENT 'SEO关键词' AFTER `seo_title`,
  ADD COLUMN `seo_description` VARCHAR(512) DEFAULT NULL COMMENT 'SEO描述' AFTER `seo_keywords`,
  ADD COLUMN `detail` LONGTEXT COMMENT '品牌介绍（富文本）' AFTER `seo_description`;

-- ============================================================
-- 12. 企业表增加字段（企业主页产品化）
-- ============================================================
ALTER TABLE `company`
  ADD COLUMN `seo_title` VARCHAR(128) DEFAULT NULL COMMENT 'SEO标题' AFTER `hot_score`,
  ADD COLUMN `seo_keywords` VARCHAR(255) DEFAULT NULL COMMENT 'SEO关键词' AFTER `seo_title`,
  ADD COLUMN `seo_description` VARCHAR(512) DEFAULT NULL COMMENT 'SEO描述' AFTER `seo_keywords`,
  ADD COLUMN `detail` LONGTEXT COMMENT '企业介绍（富文本）' AFTER `seo_description`;

-- ============================================================
-- 13. 社区帖子类型扩展
-- ============================================================
ALTER TABLE `community_post`
  ADD COLUMN `post_type` VARCHAR(32) DEFAULT 'discussion' COMMENT 'discussion/question/diy/review/news_share' AFTER `circle_id`;

-- ============================================================
-- 14. 消息表扩展类型
-- ============================================================
ALTER TABLE `message`
  MODIFY COLUMN `type` VARCHAR(32) DEFAULT 'system' COMMENT 'system/comment/reply/follow/inquiry/audit';

-- ============================================================
-- 15. 用户反馈表
-- ============================================================
DROP TABLE IF EXISTS `user_feedback`;
CREATE TABLE `user_feedback` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL,
  `type` VARCHAR(32) DEFAULT 'bug' COMMENT 'bug/feature/improvement/other',
  `title` VARCHAR(255) NOT NULL,
  `content` TEXT,
  `contact` VARCHAR(128) DEFAULT NULL COMMENT '联系方式',
  `screenshot` VARCHAR(512) DEFAULT NULL COMMENT '截图URL',
  `status` TINYINT DEFAULT 1 COMMENT '1待处理2处理中3已解决4已关闭',
  `reply` VARCHAR(1024) DEFAULT NULL COMMENT '回复',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈';

-- ============================================================
-- 16. 索引优化（Phase6 查询性能）
-- ============================================================
-- 行为事件按时间范围查询优化（已在建表时添加）
-- 排行榜快照查询优化（已在建表时添加）
-- 搜索建议权重排序优化（已在建表时添加）

-- 机器人列表查询复合索引
ALTER TABLE `robot` ADD INDEX `idx_category_status_hot` (`category_id`, `status`, `hot_score` DESC);
ALTER TABLE `robot` ADD INDEX `idx_brand_status_hot` (`brand_id`, `status`, `hot_score` DESC);

-- 品牌字母索引增强
ALTER TABLE `brand` ADD INDEX `idx_initial_status` (`initial`, `status`);

-- 询价状态查询优化
ALTER TABLE `inquiry` ADD INDEX `idx_status_create` (`status`, `create_time` DESC);

-- 评论按业务查询优化（已有 idx_biz，增加时间排序覆盖）
ALTER TABLE `comment` ADD INDEX `idx_biz_time` (`biz_type`, `biz_id`, `create_time` DESC);