-- Phase15: Schema 审计日志表
-- 记录应用层 Schema 变更审计轨迹（与 Flyway flyway_schema_history 互补）
-- 此迁移同时建立 Server 端 Flyway 迁移链（V19+，基线 V18）

CREATE TABLE IF NOT EXISTS `schema_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `version` VARCHAR(32) NOT NULL COMMENT '迁移版本号',
    `description` VARCHAR(256) NOT NULL COMMENT '变更描述',
    `checksum` VARCHAR(64) DEFAULT NULL COMMENT '校验值',
    `executed_by` VARCHAR(64) DEFAULT 'flyway' COMMENT '执行者',
    `executed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    `success` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否成功',
    `duration_ms` INT DEFAULT NULL COMMENT '执行耗时(毫秒)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schema_audit_version` (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Schema变更审计日志';

-- 记录 Phase15 基线
INSERT IGNORE INTO `schema_audit_log` (`version`, `description`, `checksum`, `executed_by`, `success`)
VALUES ('18', 'Phase1-14 基线（01-18 SQL 初始化链）', 'baseline', 'system', 1);

INSERT IGNORE INTO `schema_audit_log` (`version`, `description`, `checksum`, `executed_by`, `success`)
VALUES ('19', 'Phase15: Schema审计日志表+Flyway迁移链建立', 'v19_init', 'flyway', 1);