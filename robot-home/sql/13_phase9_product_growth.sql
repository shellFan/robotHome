-- ============================================================
-- Phase9: Product Growth & Ecosystem 数据库迁移
-- 从Phase8 DB执行此脚本必须成功
-- 字符集：utf8mb4 / 排序：utf8mb4_general_ci
-- 兼容 MySQL 5.6：无窗口函数/CTE/JSON列/utf8mb4_0900
-- ============================================================

USE robot_home;

-- ============================================================
-- 1. 用户贡献统计表（P0-5 User Profile）
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_contribution_stat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `review_count` INT NOT NULL DEFAULT 0 COMMENT '评价数',
  `post_count` INT NOT NULL DEFAULT 0 COMMENT '帖子数',
  `question_count` INT NOT NULL DEFAULT 0 COMMENT '提问数',
  `answer_count` INT NOT NULL DEFAULT 0 COMMENT '回答数',
  `helpful_received` INT NOT NULL DEFAULT 0 COMMENT '收到有帮助数',
  `correction_accepted` INT NOT NULL DEFAULT 0 COMMENT '纠错被采纳数',
  `contribution_score` INT NOT NULL DEFAULT 0 COMMENT '贡献分',
  `contributor_level` TINYINT NOT NULL DEFAULT 0 COMMENT '贡献者等级: 0普通 1贡献者 2活跃贡献者 3核心贡献者',
  `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户贡献统计';

-- ============================================================
-- 2. 增长日统计表（P0-8 Growth Analytics）
-- ============================================================
CREATE TABLE IF NOT EXISTS `growth_daily_stat` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `new_users` INT NOT NULL DEFAULT 0 COMMENT '新增用户',
  `active_users` INT NOT NULL DEFAULT 0 COMMENT '活跃用户(DAU)',
  `robot_views` INT NOT NULL DEFAULT 0 COMMENT '机器人浏览',
  `searches` INT NOT NULL DEFAULT 0 COMMENT '搜索次数',
  `favorites` INT NOT NULL DEFAULT 0 COMMENT '收藏次数',
  `compares` INT NOT NULL DEFAULT 0 COMMENT '对比次数',
  `questions` INT NOT NULL DEFAULT 0 COMMENT '提问数',
  `answers` INT NOT NULL DEFAULT 0 COMMENT '回答数',
  `posts` INT NOT NULL DEFAULT 0 COMMENT '帖子数',
  `reviews` INT NOT NULL DEFAULT 0 COMMENT '评价数',
  `selections` INT NOT NULL DEFAULT 0 COMMENT '选型次数',
  `inquiries` INT NOT NULL DEFAULT 0 COMMENT '询价数',
  `procurements` INT NOT NULL DEFAULT 0 COMMENT '采购需求数',
  `brand_views` INT NOT NULL DEFAULT 0 COMMENT '品牌浏览',
  `company_views` INT NOT NULL DEFAULT 0 COMMENT '企业浏览',
  `follows` INT NOT NULL DEFAULT 0 COMMENT '关注次数',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='增长日统计';

-- ============================================================
-- 3. 排行榜快照增强（P0-2 Ranking 2.0）
-- 添加新榜单类型: follow/favorite/discussion/review/new_product/company_attention
-- ranking_snapshot表已有uk_type_date_robot，无需改表
-- 新增ranking_weight和ranking_decay_config行
-- ============================================================
INSERT IGNORE INTO `ranking_weight` (`event_type`, `weight`, `description`, `create_time`) VALUES
('FOLLOW', 6, '关注', NOW()),
('REVIEW_CREATE', 15, '发布评价', NOW()),
('POST_CREATE', 4, '发帖', NOW()),
('QUESTION_CREATE', 3, '提问', NOW()),
('ANSWER_CREATE', 2, '回答', NOW()),
('BRAND_VIEW', 1, '品牌浏览', NOW()),
('COMPANY_VIEW', 1, '企业浏览', NOW()),
('SELECTION_SEARCH', 8, '选型搜索', NOW());

INSERT IGNORE INTO `ranking_decay_config` (`rank_type`, `half_life_days`, `min_decay_factor`, `description`, `update_time`) VALUES
('follow', 90, 0.1000, '关注榜', NOW()),
('favorite', 60, 0.1000, '收藏榜', NOW()),
('discussion', 30, 0.1500, '讨论榜', NOW()),
('review', 120, 0.0800, '口碑榜', NOW()),
('new_product', 45, 0.2000, '新品榜', NOW()),
('company_attention', 90, 0.1000, '企业关注榜', NOW());

-- ============================================================
-- 4. 排行榜快照增加排名变化字段（P0-2）
-- ============================================================
ALTER TABLE `ranking_snapshot`
  ADD COLUMN `prev_rank_no` INT DEFAULT NULL COMMENT '上次排名' AFTER `rank_no`,
  ADD COLUMN `rank_change` INT DEFAULT NULL COMMENT '排名变化(正=上升,负=下降,NULL=NEW)' AFTER `prev_rank_no`,
  ADD COLUMN `reason_code` VARCHAR(64) DEFAULT NULL COMMENT '上榜原因码' AFTER `rank_change`,
  ADD COLUMN `reason_text` VARCHAR(255) DEFAULT NULL COMMENT '上榜原因描述' AFTER `reason_code`;

-- ============================================================
-- 5. 用户表增加贡献分冗余字段（P0-5）
-- ============================================================
ALTER TABLE `user`
  ADD COLUMN `contribution_score` INT NOT NULL DEFAULT 0 COMMENT '贡献分' AFTER `post_count`,
  ADD COLUMN `review_count` INT NOT NULL DEFAULT 0 COMMENT '评价数' AFTER `contribution_score`,
  ADD COLUMN `question_count` INT NOT NULL DEFAULT 0 COMMENT '提问数' AFTER `review_count`,
  ADD COLUMN `answer_count` INT NOT NULL DEFAULT 0 COMMENT '回答数' AFTER `question_count`;

ALTER TABLE `user` ADD INDEX `idx_contribution_score` (`contribution_score` DESC);

-- ============================================================
-- 6. 品牌表增加关注数字段（P0-3 Brand Page）
-- ============================================================
ALTER TABLE `brand`
  ADD COLUMN `follow_count` INT NOT NULL DEFAULT 0 COMMENT '关注数' AFTER `robot_count`,
  ADD COLUMN `article_count` INT NOT NULL DEFAULT 0 COMMENT '文章数' AFTER `follow_count`,
  ADD COLUMN `review_count` INT NOT NULL DEFAULT 0 COMMENT '评价数(旗下机器人)' AFTER `article_count`;

ALTER TABLE `brand` ADD INDEX `idx_follow_count` (`follow_count` DESC);

-- ============================================================
-- 7. 企业表增加关注数字段（P0-4 Company Page）
-- ============================================================
ALTER TABLE `company`
  ADD COLUMN `follow_count` INT NOT NULL DEFAULT 0 COMMENT '关注数' AFTER `product_count`,
  ADD COLUMN `article_count` INT NOT NULL DEFAULT 0 COMMENT '文章数' AFTER `follow_count`;

ALTER TABLE `company` ADD INDEX `idx_follow_count` (`follow_count` DESC);

-- ============================================================
-- 8. follow表增加索引优化（P0-6 Follow Feed）
-- ============================================================
ALTER TABLE `follow`
  ADD INDEX `idx_follow_type_id` (`follow_type`, `follow_id`),
  ADD INDEX `idx_user_type_create` (`user_id`, `follow_type`, `create_time` DESC);

-- ============================================================
-- 9. behavior_event表增加索引优化（P0-8 Growth Analytics）
-- ============================================================
ALTER TABLE `behavior_event`
  ADD INDEX `idx_create_date` (`create_time`);

-- ============================================================
-- 10. robot表增加讨论数字段（P0-1 Discovery）
-- ============================================================
ALTER TABLE `robot`
  ADD COLUMN `discussion_count` INT NOT NULL DEFAULT 0 COMMENT '讨论数' AFTER `comment_count`,
  ADD COLUMN `question_count` INT NOT NULL DEFAULT 0 COMMENT '提问数' AFTER `discussion_count`,
  ADD COLUMN `review_count` INT NOT NULL DEFAULT 0 COMMENT '评价数' AFTER `question_count`,
  ADD COLUMN `follow_count` INT NOT NULL DEFAULT 0 COMMENT '关注数' AFTER `review_count`;

-- ============================================================
-- 11. community_post表增加robot_id索引（P0-1 Discovery关联查询）
-- ============================================================
ALTER TABLE `community_post`
  ADD INDEX `idx_robot_id_status` (`robot_id`, `status`);

-- ============================================================
-- 12. article表增加brand_id/company_id索引（P0-3/P0-4关联查询）
-- ============================================================
-- brand_id索引已在Phase5添加(idx_brand)
ALTER TABLE `article`
  ADD INDEX `idx_company_id` (`company_id`),
  ADD INDEX `idx_status_publish` (`status`, `publish_time` DESC);

-- ============================================================
-- 13. growth_daily_stat 初始化数据（当日）
-- ============================================================
-- 由定时任务自动填充，此处不插入

-- ============================================================
-- 14. user_contribution_stat 从现有数据初始化
-- ============================================================
INSERT IGNORE INTO `user_contribution_stat` (`user_id`, `review_count`, `post_count`, `question_count`, `answer_count`, `contribution_score`, `update_time`)
SELECT
  u.id,
  0,
  COALESCE(u.post_count, 0),
  0,
  0,
  COALESCE(u.post_count, 0) * 3,
  NOW()
FROM `user` u
WHERE u.deleted = 0 AND u.status = 1;

-- ============================================================
-- 15. 索引审计补充
-- ============================================================
-- robot按发布日期（新品榜）
ALTER TABLE `robot` ADD INDEX `idx_release_date_status` (`release_date` DESC, `status`);

-- robot按评分（口碑榜）
ALTER TABLE `robot` ADD INDEX `idx_score_status` (`score` DESC, `status`);

-- robot按关注数
ALTER TABLE `robot` ADD INDEX `idx_follow_count_status` (`follow_count` DESC, `status`);