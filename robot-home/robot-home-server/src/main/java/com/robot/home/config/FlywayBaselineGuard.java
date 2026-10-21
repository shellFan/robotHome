package com.robot.home.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Flyway Baseline 安全守卫
 *
 * 防止不完整的数据库被错误 baseline 到 V18：
 * - 空数据库 → 允许（全新部署，docker-entrypoint-initdb.d 先执行 01-18）
 * - 非空库 + 已有 flyway_schema_history → 正常 migrate
 * - 非空库 + 无 flyway_schema_history → 校验 Schema 指纹：
 *   - 指纹完整（Phase13/14 关键表齐全）→ 允许 baseline
 *   - 指纹不完整 → 拒绝启动，抛出明确错误
 *
 * 兼容：Java 8 / Spring Boot 2.x / MySQL 5.6 / H2 (MODE=MySQL)
 */
@Configuration
public class FlywayBaselineGuard {

    private static final Logger log = LoggerFactory.getLogger(FlywayBaselineGuard.class);

    /** Phase13/14 商业化必需表 — baseline 前必须全部存在 */
    private static final String[] REQUIRED_PHASE13_TABLES = {
            "enterprise_company", "procurement_lead"
    };

    private static final String[] REQUIRED_PHASE14_TABLES = {
            "enterprise_plan", "enterprise_subscription", "enterprise_entitlement",
            "enterprise_order", "advertising_slot", "advertising_campaign"
    };

    /** 核心业务表（Phase1-12）— baseline 前必须存在 */
    private static final String[] REQUIRED_CORE_TABLES = {
            "robot", "brand", "company", "article"
    };

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(DataSource dataSource, Environment environment) {
        return flyway -> {
            validateBeforeBaseline(dataSource);
            flyway.migrate();
            log.info("Flyway 迁移完成，schema 版本已就绪");
        };
    }

    /**
     * 在 Flyway migrate 前校验数据库状态
     */
    private void validateBeforeBaseline(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            validateDatabaseForBaseline(conn);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Flyway Baseline Guard: 数据库状态检查失败 — " + e.getMessage(), e);
        }
    }

    /**
     * 校验数据库是否允许 baseline（包级可见，供测试直接调用）
     *
     * 规则：
     * - 空数据库 → 允许（全新初始化）
     * - 已有 flyway_schema_history → 允许（增量迁移）
     * - 非空库无 Flyway 历史 → 必须通过 Schema 指纹校验
     */
    static void validateDatabaseForBaseline(Connection conn) throws Exception {
        boolean hasFlywayHistory = tableExists(conn, "flyway_schema_history");
        boolean databaseEmpty = isDatabaseEmpty(conn);

        if (databaseEmpty) {
            log.info("Flyway Baseline Guard: 数据库为空，允许全新初始化");
            return;
        }

        if (hasFlywayHistory) {
            log.info("Flyway Baseline Guard: 已有 flyway_schema_history，执行增量迁移");
            return;
        }

        // 非空库 + 无 Flyway 历史 → 将触发 baseline-on-migrate，必须校验指纹
        log.info("Flyway Baseline Guard: 非空库无 Flyway 历史，执行 Schema 指纹校验...");
        validateSchemaFingerprint(conn);
    }

    /**
     * 校验 Schema 指纹：必需表必须全部存在
     * 不满足时拒绝启动，防止不完整库被错误 baseline
     */
    private static void validateSchemaFingerprint(Connection conn) throws Exception {
        List<String> missing = new ArrayList<>();

        for (String table : REQUIRED_CORE_TABLES) {
            if (!tableExists(conn, table)) {
                missing.add(table);
            }
        }
        for (String table : REQUIRED_PHASE13_TABLES) {
            if (!tableExists(conn, table)) {
                missing.add(table);
            }
        }
        for (String table : REQUIRED_PHASE14_TABLES) {
            if (!tableExists(conn, table)) {
                missing.add(table);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "Flyway Baseline Guard: 数据库非空但缺少必需表 %s。" +
                    "Flyway baseline-on-migrate 会将此库标记为 V18 已最新，缺失的表将永远不会被创建。" +
                    "请先执行完整的 SQL 初始化链（01-18），或修复数据库后重试。",
                    missing));
        }

        log.info("Flyway Baseline Guard: Schema 指纹校验通过（核心+Phase13+Phase14 必需表齐全），允许 baseline");
    }

    /**
     * 检查数据库是否有任何用户表
     */
    private static boolean isDatabaseEmpty(Connection conn) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        String catalog = conn.getCatalog();
        Set<String> userTables = new HashSet<>();
        try (ResultSet rs = meta.getTables(catalog, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String schema = rs.getString("TABLE_SCHEM");
                if (tableName == null) {
                    continue;
                }
                // 排除 Flyway 内部表和 H2/MySQL 系统表
                String upper = tableName.toUpperCase();
                if (upper.startsWith("FLYWAY_")) {
                    continue;
                }
                // H2 系统表在 INFORMATION_SCHEMA / PG_CATALOG 等系统 schema 中
                if (schema != null) {
                    String upperSchema = schema.toUpperCase();
                    if (upperSchema.startsWith("INFORMATION_") || upperSchema.startsWith("PG_")
                            || upperSchema.equals("MYSQL") || upperSchema.equals("SYS")) {
                        continue;
                    }
                }
                userTables.add(tableName.toLowerCase());
            }
        }
        return userTables.isEmpty();
    }

    /**
     * 检查表是否存在（兼容 MySQL 和 H2 大小写）
     */
    private static boolean tableExists(Connection conn, String tableName) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        String catalog = conn.getCatalog();
        // H2 CASE_INSENSITIVE_IDENTIFIERS=TRUE 时表名大写存储
        String[] candidates = {tableName, tableName.toUpperCase(), tableName.toLowerCase()};
        for (String candidate : candidates) {
            try (ResultSet rs = meta.getTables(catalog, null, candidate, new String[]{"TABLE"})) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }
}