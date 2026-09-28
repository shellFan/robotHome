-- ============================================================
-- Phase12: Real Data & Content Growth — H2测试兼容版
-- 仅包含H2可执行的ALTER TABLE ADD COLUMN语句
-- MySQL存储过程(p_add_column/p_add_index)在H2中自动跳过
-- ============================================================

-- P0-1: crawler_source 健康度持久化字段
ALTER TABLE crawler_source ADD COLUMN consecutive_failures INT DEFAULT 0;
ALTER TABLE crawler_source ADD COLUMN avg_latency_ms INT DEFAULT 0;
ALTER TABLE crawler_source ADD COLUMN last_success_time TIMESTAMP;
ALTER TABLE crawler_source ADD COLUMN last_fail_time TIMESTAMP;
ALTER TABLE crawler_source ADD COLUMN last_fail_reason VARCHAR(512);
ALTER TABLE crawler_source ADD COLUMN total_crawls INT DEFAULT 0;
ALTER TABLE crawler_source ADD COLUMN total_success_crawls INT DEFAULT 0;
ALTER TABLE crawler_source ADD COLUMN robots_checked TINYINT DEFAULT 0;
ALTER TABLE crawler_source ADD COLUMN robots_allowed TINYINT;
ALTER TABLE crawler_source ADD COLUMN robots_checked_time TIMESTAMP;

-- P0-2: crawler_source 信任等级
ALTER TABLE crawler_source ADD COLUMN trust_level VARCHAR(16) DEFAULT 'NORMAL';

-- P0-1: robot Freshness 字段
ALTER TABLE robot ADD COLUMN freshness VARCHAR(16) DEFAULT 'STALE';
ALTER TABLE robot ADD COLUMN last_content_update_time TIMESTAMP;