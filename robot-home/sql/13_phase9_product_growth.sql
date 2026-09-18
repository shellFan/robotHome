-- ============================================================
-- Phase9: Product Growth & Ecosystem 数据库迁移
-- 从Phase8 DB执行此脚本必须成功
-- 字符集：utf8mb4 / 排序：utf8mb4_general_ci
-- 兼容 MySQL 5.6：无窗口函数/CTE/JSON列/utf8mb4_0900
-- 幂等性：所有ALTER TABLE ADD COLUMN/ADD INDEX均带IF NOT EXISTS保护
-- ============================================================

USE robot_home;

-- ============================================================
-- 幂等性辅助：添加列（如果不存在则添加）
-- 注意：MySQL 5.6不支持ALTER TABLE ... ADD COLUMN IF NOT EXISTS
-- 使用information_schema检查实现幂等
-- ============================================================

DELIMITER $$

-- 安全添加列的存储过程
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

-- 安全添加索引的存储过程
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
-- 4. 排行榜快照增加排名变化字段（P0-2）— 幂等
-- ============================================================
CALL `p_add_column`('ranking_snapshot', 'prev_rank_no', 'INT DEFAULT NULL COMMENT ''上次排名'' AFTER `rank_no`');
CALL `p_add_column`('ranking_snapshot', 'rank_change', 'INT DEFAULT NULL COMMENT ''排名变化(正=上升,负=下降,NULL=NEW)'' AFTER `prev_rank_no`');
CALL `p_add_column`('ranking_snapshot', 'reason_code', 'VARCHAR(64) DEFAULT NULL COMMENT ''上榜原因码'' AFTER `rank_change`');
CALL `p_add_column`('ranking_snapshot', 'reason_text', 'VARCHAR(255) DEFAULT NULL COMMENT ''上榜原因描述'' AFTER `reason_code`');

-- ============================================================
-- 5. 用户表增加贡献分冗余字段（P0-5）— 幂等
-- ============================================================
CALL `p_add_column`('user', 'contribution_score', 'INT NOT NULL DEFAULT 0 COMMENT ''贡献分'' AFTER `post_count`');
CALL `p_add_column`('user', 'review_count', 'INT NOT NULL DEFAULT 0 COMMENT ''评价数'' AFTER `contribution_score`');
CALL `p_add_column`('user', 'question_count', 'INT NOT NULL DEFAULT 0 COMMENT ''提问数'' AFTER `review_count`');
CALL `p_add_column`('user', 'answer_count', 'INT NOT NULL DEFAULT 0 COMMENT ''回答数'' AFTER `question_count`');

CALL `p_add_index`('user', 'idx_contribution_score', '`contribution_score`');

-- ============================================================
-- 6. 品牌表增加关注数字段（P0-3 Brand Page）— 幂等
-- ============================================================
CALL `p_add_column`('brand', 'follow_count', 'INT NOT NULL DEFAULT 0 COMMENT ''关注数'' AFTER `robot_count`');
CALL `p_add_column`('brand', 'article_count', 'INT NOT NULL DEFAULT 0 COMMENT ''文章数'' AFTER `follow_count`');
CALL `p_add_column`('brand', 'review_count', 'INT NOT NULL DEFAULT 0 COMMENT ''评价数(旗下机器人)'' AFTER `article_count`');

CALL `p_add_index`('brand', 'idx_follow_count', '`follow_count`');

-- ============================================================
-- 7. 企业表增加关注数字段（P0-4 Company Page）— 幂等
-- ============================================================
CALL `p_add_column`('company', 'follow_count', 'INT NOT NULL DEFAULT 0 COMMENT ''关注数'' AFTER `product_count`');
CALL `p_add_column`('company', 'article_count', 'INT NOT NULL DEFAULT 0 COMMENT ''文章数'' AFTER `follow_count`');

CALL `p_add_index`('company', 'idx_follow_count', '`follow_count`');

-- ============================================================
-- 8. follow表增加索引优化（P0-6 Follow Feed）— 幂等
-- ============================================================
CALL `p_add_index`('follow', 'idx_follow_type_id', '`follow_type`, `follow_id`');
CALL `p_add_index`('follow', 'idx_user_type_create', '`user_id`, `follow_type`, `create_time`');

-- ============================================================
-- 9. behavior_event表增加索引优化（P0-8 Growth Analytics）— 幂等
-- ============================================================
CALL `p_add_index`('behavior_event', 'idx_create_date', '`create_time`');

-- ============================================================
-- 10. robot表增加讨论数字段（P0-1 Discovery）— 幂等
-- ============================================================
CALL `p_add_column`('robot', 'discussion_count', 'INT NOT NULL DEFAULT 0 COMMENT ''讨论数'' AFTER `comment_count`');
CALL `p_add_column`('robot', 'question_count', 'INT NOT NULL DEFAULT 0 COMMENT ''提问数'' AFTER `discussion_count`');
CALL `p_add_column`('robot', 'review_count', 'INT NOT NULL DEFAULT 0 COMMENT ''评价数'' AFTER `question_count`');
CALL `p_add_column`('robot', 'follow_count', 'INT NOT NULL DEFAULT 0 COMMENT ''关注数'' AFTER `review_count`');

-- ============================================================
-- 11. community_post表增加robot_id索引（P0-1 Discovery关联查询）— 幂等
-- ============================================================
CALL `p_add_index`('community_post', 'idx_robot_id_status', '`robot_id`, `status`');

-- ============================================================
-- 12. article表增加索引（P0-3/P0-4关联查询）— 幂等
-- ============================================================
CALL `p_add_index`('article', 'idx_company_id', '`company_id`');
CALL `p_add_index`('article', 'idx_status_publish', '`status`, `publish_time`');

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
-- 15. 索引审计补充 — 幂等
-- 注意：MySQL 5.6不支持DESC索引，DESC关键字被解析但忽略
-- 实际排序由ORDER BY在查询时保证
-- ============================================================
CALL `p_add_index`('robot', 'idx_release_date_status', '`release_date`, `status`');
CALL `p_add_index`('robot', 'idx_score_status', '`score`, `status`');
CALL `p_add_index`('robot', 'idx_follow_count_status', '`follow_count`, `status`');

-- ============================================================
-- 16. 清理辅助存储过程
-- ============================================================
DROP PROCEDURE IF EXISTS `p_add_column`;
DROP PROCEDURE IF EXISTS `p_add_index`;