-- ============================================================
-- Phase9 测试数据: Product Growth & Ecosystem
-- H2兼容 / MySQL 5.6兼容
-- ============================================================

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