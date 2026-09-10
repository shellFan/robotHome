-- ============================================================
-- 机器人之家 Phase 5 - 采集数据源配置（25+ 机器人官网）
-- 兼容 MySQL 5.6
-- 关联品牌ID来自 06_real_brands_companies.sql
-- ============================================================
USE robot_home;

-- ============================================================
-- 中国人形机器人企业官网（10个）
-- ============================================================

INSERT INTO `crawler_source` (`id`, `source_name`, `source_type`, `base_url`, `brand_id`, `company_id`, `crawl_enabled`, `crawl_strategy`, `crawl_interval`, `max_depth`, `max_pages`, `include_rules`, `exclude_rules`, `title_selector`, `content_selector`, `date_selector`, `author_selector`, `auto_publish`, `respect_robots`, `request_delay`, `seed_urls`, `sitemap_url`, `rss_url`, `fetch_mode`, `follow_external`, `status`, `language`, `region`, `priority`, `tags`, `health_status`, `create_time`, `update_time`) VALUES
(1, '宇树科技官网', 'WEBSITE', 'https://www.unitree.com', 101, 101, 1, 'GENERIC', 86400, 2, 200, '/news/,/blog/,/product/', '/login,/register,/cart,/checkout,/search', 'h1,h2.post-title', 'article,.post-content,.entry-content,.content', 'time,.date,.post-date', '.author,.post-author', 0, 1, 2000, NULL, 'https://www.unitree.com/sitemap.xml', NULL, 'http', 0, 1, 'zh', 'CN', 9, '人形机器人,四足机器人', 'UNKNOWN', NOW(), NOW()),

(2, '优必选官网', 'WEBSITE', 'https://www.ubtrobot.com', 102, 102, 1, 'GENERIC', 86400, 2, 200, '/news/,/blog/,/product/', '/login,/register,/cart,/search', 'h1,h2.title', 'article,.content,.post-body', 'time,.date,.post-date', '.author', 0, 1, 2000, NULL, 'https://www.ubtrobot.com/sitemap.xml', NULL, 'http', 0, 1, 'zh', 'CN', 9, '人形机器人,AI教育', 'UNKNOWN', NOW(), NOW()),

(3, '智元机器人官网', 'WEBSITE', 'https://www.agibot.com', 103, 103, 1, 'GENERIC', 86400, 2, 150, '/news/,/blog/,/product/', '/login,/register,/search', 'h1,h2', 'article,.content,.post-content', 'time,.date', '.author', 0, 1, 2000, NULL, NULL, NULL, 'http', 0, 1, 'zh', 'CN', 8, '人形机器人,具身智能', 'UNKNOWN', NOW(), NOW()),

(4, '傅利叶智能官网', 'WEBSITE', 'https://www.fftai.com', 104, 104, 1, 'GENERIC', 86400, 2, 150, '/news/,/blog/,/product/', '/login,/register,/search', 'h1,h2', 'article,.content,.entry', 'time,.date', '.author', 0, 1, 2000, NULL, NULL, NULL, 'http', 0, 1, 'zh', 'CN', 8, '康复机器人,人形机器人', 'UNKNOWN', NOW(), NOW()),

(5, '追觅科技官网', 'WEBSITE', 'https://www.dreame.tech', 115, 115, 1, 'GENERIC', 86400, 2, 200, '/news/,/blog/,/product/', '/login,/register,/cart,/checkout,/search', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 2000, NULL, 'https://www.dreame.tech/sitemap.xml', NULL, 'http', 0, 1, 'zh', 'CN', 7, '清洁机器人,人形机器人', 'UNKNOWN', NOW(), NOW()),

(6, '开普勒机器人官网', 'WEBSITE', 'https://www.keplerbot.com', 116, 116, 1, 'GENERIC', 86400, 2, 100, '/news/,/blog/,/product/', '/login,/register,/search', 'h1,h2', 'article,.content', 'time,.date', '.author', 0, 1, 2000, NULL, NULL, NULL, 'http', 0, 1, 'zh', 'CN', 7, '人形机器人', 'UNKNOWN', NOW(), NOW()),

(7, '乐聚机器人官网', 'WEBSITE', 'https://www.lejurobot.com', 118, 118, 1, 'GENERIC', 86400, 2, 150, '/news/,/blog/,/product/', '/login,/register,/search', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 2000, NULL, NULL, NULL, 'http', 0, 1, 'zh', 'CN', 7, '人形机器人,AI教育', 'UNKNOWN', NOW(), NOW()),

(8, '达闼科技官网', 'WEBSITE', 'https://www.cloudminds.com', 110, 110, 1, 'GENERIC', 86400, 2, 150, '/news/,/blog/,/product/,/solution/', '/login,/register,/search', 'h1,h2', 'article,.content,.entry', 'time,.date', '.author', 0, 1, 2000, NULL, NULL, NULL, 'http', 0, 1, 'zh', 'CN', 7, '云端智能,人形机器人', 'UNKNOWN', NOW(), NOW()),

(9, '小鹏鹏行官网', 'WEBSITE', 'https://www.xpeng.com', 114, 114, 1, 'GENERIC', 86400, 2, 200, '/news/,/blog/,/xpeng-robotics/', '/login,/register,/cart,/checkout,/configurator', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 2000, NULL, 'https://www.xpeng.com/sitemap.xml', NULL, 'http', 0, 1, 'zh', 'CN', 8, '人形机器人,汽车', 'UNKNOWN', NOW(), NOW()),

(10, '星动纪元官网', 'WEBSITE', 'https://www.starera.com', 117, 117, 1, 'GENERIC', 86400, 2, 100, '/news/,/blog/,/product/', '/login,/register,/search', 'h1,h2', 'article,.content', 'time,.date', '.author', 0, 1, 2000, NULL, NULL, NULL, 'http', 0, 1, 'zh', 'CN', 6, '人形机器人', 'UNKNOWN', NOW(), NOW());

-- ============================================================
-- 国际人形机器人企业官网（8个）
-- ============================================================

INSERT INTO `crawler_source` (`id`, `source_name`, `source_type`, `base_url`, `brand_id`, `company_id`, `crawl_enabled`, `crawl_strategy`, `crawl_interval`, `max_depth`, `max_pages`, `include_rules`, `exclude_rules`, `title_selector`, `content_selector`, `date_selector`, `author_selector`, `auto_publish`, `respect_robots`, `request_delay`, `seed_urls`, `sitemap_url`, `rss_url`, `fetch_mode`, `follow_external`, `status`, `language`, `region`, `priority`, `tags`, `health_status`, `create_time`, `update_time`) VALUES
(11, 'Boston Dynamics官网', 'WEBSITE', 'https://www.bostondynamics.com', 111, 111, 1, 'GENERIC', 86400, 2, 200, '/blog/,/news/,/products/,/resources/', '/login,/register,/cart,/careers', 'h1,h2', 'article,.content,.post-body,.entry-content', 'time,.date,.post-date', '.author,.post-author', 0, 1, 3000, NULL, 'https://www.bostondynamics.com/sitemap.xml', NULL, 'http', 0, 1, 'en', 'US', 10, '人形机器人,四足机器人,Atlas,Spot', 'UNKNOWN', NOW(), NOW()),

(12, 'Figure AI官网', 'WEBSITE', 'https://www.figure.ai', 112, 112, 1, 'GENERIC', 86400, 2, 100, '/blog/,/news/,/updates/', '/login,/register,/careers', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 3000, NULL, NULL, NULL, 'http', 0, 1, 'en', 'US', 9, '人形机器人,Figure', 'UNKNOWN', NOW(), NOW()),

(13, 'Agility Robotics官网', 'WEBSITE', 'https://www.agilityrobotics.com', 113, 113, 1, 'GENERIC', 86400, 2, 100, '/blog/,/news/,/products/', '/login,/register,/careers', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 3000, NULL, NULL, NULL, 'http', 0, 1, 'en', 'US', 8, '人形机器人,Digit', 'UNKNOWN', NOW(), NOW()),

(14, '1X Technologies官网', 'WEBSITE', 'https://www.1x.tech', 126, 126, 1, 'GENERIC', 86400, 2, 100, '/blog/,/news/,/updates/', '/login,/register,/careers', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 3000, NULL, NULL, NULL, 'http', 0, 1, 'en', 'EU', 8, '人形机器人,NEO,Eve', 'UNKNOWN', NOW(), NOW()),

(15, 'Sanctuary AI官网', 'WEBSITE', 'https://www.sanctuary.ai', 127, 127, 1, 'GENERIC', 86400, 2, 100, '/blog/,/news/,/updates/', '/login,/register,/careers', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 3000, NULL, NULL, NULL, 'http', 0, 1, 'en', 'CA', 8, '人形机器人,Phoenix', 'UNKNOWN', NOW(), NOW()),

(16, 'Apptronik官网', 'WEBSITE', 'https://www.apptronik.com', 128, 128, 1, 'GENERIC', 86400, 2, 100, '/blog/,/news/,/products/', '/login,/register,/careers', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 3000, NULL, NULL, NULL, 'http', 0, 1, 'en', 'US', 7, '人形机器人,Apollo', 'UNKNOWN', NOW(), NOW()),

(17, 'Tesla Optimus', 'WEBSITE', 'https://www.tesla.com/optimus', 125, 125, 1, 'GENERIC', 86400, 2, 100, '/optimus/,/blog/', '/login,/register,/cart,/order', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 3000, NULL, 'https://www.tesla.com/sitemap.xml', NULL, 'http', 0, 1, 'en', 'US', 10, '人形机器人,Optimus,Tesla', 'UNKNOWN', NOW(), NOW()),

(18, '丰田机器人官网', 'WEBSITE', 'https://robotics.toyota', 123, 123, 1, 'GENERIC', 86400, 2, 100, '/news/,/projects/,/research/', '/login,/register', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 3000, NULL, NULL, NULL, 'http', 0, 1, 'en', 'JP', 7, '人形机器人,辅助机器人', 'UNKNOWN', NOW(), NOW());

-- ============================================================
-- 工业机器人四大家族官网（4个）
-- ============================================================

INSERT INTO `crawler_source` (`id`, `source_name`, `source_type`, `base_url`, `brand_id`, `company_id`, `crawl_enabled`, `crawl_strategy`, `crawl_interval`, `max_depth`, `max_pages`, `include_rules`, `exclude_rules`, `title_selector`, `content_selector`, `date_selector`, `author_selector`, `auto_publish`, `respect_robots`, `request_delay`, `seed_urls`, `sitemap_url`, `rss_url`, `fetch_mode`, `follow_external`, `status`, `language`, `region`, `priority`, `tags`, `health_status`, `create_time`, `update_time`) VALUES
(19, 'FANUC官网', 'WEBSITE', 'https://www.fanuc.com', 131, NULL, 1, 'GENERIC', 86400, 2, 200, '/products/,/news/,/blog/,/case-study/', '/login,/register,/cart,/contact', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 3000, NULL, 'https://www.fanuc.com/sitemap.xml', NULL, 'http', 0, 1, 'en', 'JP', 8, '工业机器人,CNC,FANUC', 'UNKNOWN', NOW(), NOW()),

(20, 'ABB机器人官网', 'WEBSITE', 'https://new.abb.com/products/robotics', 132, NULL, 1, 'GENERIC', 86400, 2, 200, '/products/robotics/,/news/,/blog/,/case-study/', '/login,/register,/cart', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 3000, NULL, NULL, NULL, 'http', 0, 1, 'en', 'EU', 8, '工业机器人,ABB,协作机器人', 'UNKNOWN', NOW(), NOW()),

(21, 'KUKA官网', 'WEBSITE', 'https://www.kuka.com', 133, NULL, 1, 'GENERIC', 86400, 2, 200, '/news/,/blog/,/products/,/industries/', '/login,/register,/cart,/careers', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 3000, NULL, 'https://www.kuka.com/sitemap.xml', NULL, 'http', 0, 1, 'en', 'DE', 8, '工业机器人,KUKA,协作机器人', 'UNKNOWN', NOW(), NOW()),

(22, '安川电机官网', 'WEBSITE', 'https://www.yaskawa.com', 134, NULL, 1, 'GENERIC', 86400, 2, 200, '/products/,/news/,/blog/,/case-study/', '/login,/register,/cart', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 3000, NULL, 'https://www.yaskawa.com/sitemap.xml', NULL, 'http', 0, 1, 'en', 'JP', 8, '工业机器人,安川,Motoman', 'UNKNOWN', NOW(), NOW());

-- ============================================================
-- 服务机器人企业官网（3个）
-- ============================================================

INSERT INTO `crawler_source` (`id`, `source_name`, `source_type`, `base_url`, `brand_id`, `company_id`, `crawl_enabled`, `crawl_strategy`, `crawl_interval`, `max_depth`, `max_pages`, `include_rules`, `exclude_rules`, `title_selector`, `content_selector`, `date_selector`, `author_selector`, `auto_publish`, `respect_robots`, `request_delay`, `seed_urls`, `sitemap_url`, `rss_url`, `fetch_mode`, `follow_external`, `status`, `language`, `region`, `priority`, `tags`, `health_status`, `create_time`, `update_time`) VALUES
(23, '普渡机器人官网', 'WEBSITE', 'https://www.pudurobotics.com', 105, 105, 1, 'GENERIC', 86400, 2, 150, '/news/,/blog/,/product/', '/login,/register,/search', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 2000, NULL, 'https://www.pudurobotics.com/sitemap.xml', NULL, 'http', 0, 1, 'zh', 'CN', 7, '服务机器人,配送机器人', 'UNKNOWN', NOW(), NOW()),

(24, '擎朗智能官网', 'WEBSITE', 'https://www.keenonrobotics.com', 106, 106, 1, 'GENERIC', 86400, 2, 150, '/news/,/blog/,/product/,/solution/', '/login,/register,/search', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 2000, NULL, NULL, NULL, 'http', 0, 1, 'zh', 'CN', 7, '服务机器人,配送机器人', 'UNKNOWN', NOW(), NOW()),

(25, '猎户星空官网', 'WEBSITE', 'https://www.orionstar.com', 119, 119, 1, 'GENERIC', 86400, 2, 150, '/news/,/blog/,/product/,/solution/', '/login,/register,/search', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 2000, NULL, 'https://www.orionstar.com/sitemap.xml', NULL, 'http', 0, 1, 'zh', 'CN', 7, '服务机器人,清洁机器人', 'UNKNOWN', NOW(), NOW());

-- ============================================================
-- 协作机器人企业官网（3个）
-- ============================================================

INSERT INTO `crawler_source` (`id`, `source_name`, `source_type`, `base_url`, `brand_id`, `company_id`, `crawl_enabled`, `crawl_strategy`, `crawl_interval`, `max_depth`, `max_pages`, `include_rules`, `exclude_rules`, `title_selector`, `content_selector`, `date_selector`, `author_selector`, `auto_publish`, `respect_robots`, `request_delay`, `seed_urls`, `sitemap_url`, `rss_url`, `fetch_mode`, `follow_external`, `status`, `language`, `region`, `priority`, `tags`, `health_status`, `create_time`, `update_time`) VALUES
(26, '越疆科技官网', 'WEBSITE', 'https://www.dobot.cc', 107, 107, 1, 'GENERIC', 86400, 2, 150, '/news/,/blog/,/product/', '/login,/register,/search', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 2000, NULL, 'https://www.dobot.cc/sitemap.xml', NULL, 'http', 0, 1, 'zh', 'CN', 7, '协作机器人,机械臂', 'UNKNOWN', NOW(), NOW()),

(27, '节卡机器人官网', 'WEBSITE', 'https://www.jaka.com', 108, 108, 1, 'GENERIC', 86400, 2, 150, '/news/,/blog/,/product/,/solution/', '/login,/register,/search', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 2000, NULL, NULL, NULL, 'http', 0, 1, 'zh', 'CN', 7, '协作机器人', 'UNKNOWN', NOW(), NOW()),

(28, '遨博智能官网', 'WEBSITE', 'https://www.aubo-robotics.com', 109, 109, 1, 'GENERIC', 86400, 2, 150, '/news/,/blog/,/product/', '/login,/register,/search', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 2000, NULL, NULL, NULL, 'http', 0, 1, 'zh', 'CN', 6, '协作机器人', 'UNKNOWN', NOW(), NOW());

-- ============================================================
-- 其他机器人企业官网（2个）
-- ============================================================

INSERT INTO `crawler_source` (`id`, `source_name`, `source_type`, `base_url`, `brand_id`, `company_id`, `crawl_enabled`, `crawl_strategy`, `crawl_interval`, `max_depth`, `max_pages`, `include_rules`, `exclude_rules`, `title_selector`, `content_selector`, `date_selector`, `author_selector`, `auto_publish`, `respect_robots`, `request_delay`, `seed_urls`, `sitemap_url`, `rss_url`, `fetch_mode`, `follow_external`, `status`, `language`, `region`, `priority`, `tags`, `health_status`, `create_time`, `update_time`) VALUES
(29, '高仙机器人官网', 'WEBSITE', 'https://www.gaussianrobotics.com', 121, 121, 1, 'GENERIC', 86400, 2, 150, '/news/,/blog/,/product/', '/login,/register,/search', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 2000, NULL, NULL, NULL, 'http', 0, 1, 'zh', 'CN', 6, '清洁机器人,商用机器人', 'UNKNOWN', NOW(), NOW()),

(30, '九号机器人官网', 'WEBSITE', 'https://www.ninebot.com', 122, 122, 1, 'GENERIC', 86400, 2, 150, '/news/,/blog/,/product/', '/login,/register,/search', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 2000, NULL, 'https://www.ninebot.com/sitemap.xml', NULL, 'http', 0, 1, 'zh', 'CN', 6, '配送机器人,短交通', 'UNKNOWN', NOW(), NOW());

-- ============================================================
-- 机器人行业新闻媒体（3个RSS源）
-- ============================================================

INSERT INTO `crawler_source` (`id`, `source_name`, `source_type`, `base_url`, `brand_id`, `company_id`, `crawl_enabled`, `crawl_strategy`, `crawl_interval`, `max_depth`, `max_pages`, `include_rules`, `exclude_rules`, `title_selector`, `content_selector`, `date_selector`, `author_selector`, `auto_publish`, `respect_robots`, `request_delay`, `seed_urls`, `sitemap_url`, `rss_url`, `fetch_mode`, `follow_external`, `status`, `language`, `region`, `priority`, `tags`, `health_status`, `create_time`, `update_time`) VALUES
(31, 'The Robot Report', 'RSS', 'https://www.therobotreport.com', NULL, NULL, 1, 'RSS', 3600, 1, 100, NULL, '/login,/register,/subscribe', 'h1.entry-title', '.entry-content,article', '.entry-date,time', '.author,.post-author', 0, 1, 3000, NULL, NULL, 'https://www.therobotreport.com/feed/', 'http', 0, 1, 'en', 'US', 8, '行业新闻,机器人资讯', 'UNKNOWN', NOW(), NOW()),

(32, 'IEEE Spectrum Robotics', 'RSS', 'https://spectrum.ieee.org/robotics', NULL, NULL, 1, 'RSS', 3600, 1, 100, NULL, '/login,/register,/subscribe', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 3000, NULL, NULL, 'https://spectrum.ieee.org/feeds/feed.rss', 'http', 0, 1, 'en', 'US', 8, '学术,机器人研究', 'UNKNOWN', NOW(), NOW()),

(33, 'Robotics Business Review', 'WEBSITE', 'https://roboticsbusinessreview.com', NULL, NULL, 1, 'GENERIC', 86400, 2, 150, '/articles/,/news/,/features/', '/login,/register,/subscribe', 'h1,h2', 'article,.content,.post-body', 'time,.date', '.author', 0, 1, 3000, NULL, NULL, NULL, 'http', 0, 1, 'en', 'US', 7, '行业新闻,商业机器人', 'UNKNOWN', NOW(), NOW());

-- ============================================================
-- 采集源索引
-- ============================================================
ALTER TABLE `crawler_source` ADD INDEX `idx_source_type` (`source_type`);
ALTER TABLE `crawler_source` ADD INDEX `idx_crawl_enabled` (`crawl_enabled`);
ALTER TABLE `crawler_source` ADD INDEX `idx_region` (`region`);
ALTER TABLE `crawler_source` ADD INDEX `idx_language` (`language`);