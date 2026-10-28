package com.robot.home.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Flyway Baseline Guard 测试 — 覆盖 DB-11 ~ DB-14 场景
 *
 * DB-11 完整历史库（核心+Phase13+Phase14表齐全）→ 允许 baseline
 * DB-12 缺少Phase13/14表的不完整库 → 拒绝 baseline
 * DB-13 空数据库 → 拒绝并要求先执行 01-18 SQL 初始化链
 * DB-14 仅有 flyway_schema_history 的不完整库 → 拒绝升级
 */
@DisplayName("Flyway Baseline Guard DB-11~DB-14")
class FlywayBaselineGuardTest {

    private String h2Url;

    @BeforeEach
    void setUp() {
        h2Url = "jdbc:h2:mem:guard_test_" + System.nanoTime()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1";
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "");
             Statement st = conn.createStatement()) {
            st.execute("SHUTDOWN");
        } catch (Exception ignored) {
        }
    }

    private void createTable(Connection conn, String name) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE `" + name + "` (`id` BIGINT AUTO_INCREMENT PRIMARY KEY, `name` VARCHAR(128))");
        }
    }

    private void addColumns(Connection conn, String table, String... definitions) throws Exception {
        try (Statement st = conn.createStatement()) {
            for (String definition : definitions) {
                st.execute("ALTER TABLE `" + table + "` ADD COLUMN " + definition);
            }
        }
    }

    private void addIndex(Connection conn, String table, String index, String columns) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE UNIQUE INDEX `" + index + "` ON `" + table + "` (" + columns + ")");
        }
    }

    /** 创建实际 SQL 初始化链中的全部必需表（核心+Phase13+Phase14） */
    private void createAllRequiredTables(Connection conn) throws Exception {
        // 核心
        for (String t : new String[]{"robot", "brand", "company", "article", "crawler_source"}) {
            createTable(conn, t);
        }
        // Phase13
        for (String t : new String[]{"company_claim", "company_profile_change"}) {
            createTable(conn, t);
        }
        addColumns(conn, "company_claim",
                "`company_id` BIGINT", "`applicant_user_id` BIGINT", "`proof_materials` TEXT",
                "`pending_company_id` BIGINT", "`pending_applicant_user_id` BIGINT");
        addIndex(conn, "company_claim", "uk_company_active_claim", "`pending_company_id`");
        addIndex(conn, "company_claim", "uk_applicant_active_claim", "`pending_applicant_user_id`");
        addColumns(conn, "company_profile_change",
                "`company_id` BIGINT", "`submitter_user_id` BIGINT", "`new_value` TEXT");
        // Phase14
        for (String t : new String[]{"enterprise_plan", "enterprise_subscription",
                "enterprise_entitlement", "commercial_order", "advertising_slot", "advertising_campaign"}) {
            createTable(conn, t);
        }
        addColumns(conn, "enterprise_plan",
                "`code` VARCHAR(32)", "`current_version` INT", "`saleable` TINYINT", "`deleted` TINYINT");
        addIndex(conn, "enterprise_plan", "uk_plan_code", "`code`, `deleted`");
        addColumns(conn, "enterprise_subscription",
                "`company_id` BIGINT", "`plan_version_id` BIGINT", "`source_order_id` BIGINT",
                "`status` VARCHAR(24)", "`deleted` TINYINT");
        addIndex(conn, "enterprise_subscription", "uk_company_subscription", "`company_id`, `deleted`");
        addColumns(conn, "enterprise_entitlement",
                "`company_id` BIGINT", "`subscription_id` BIGINT", "`ent_code` VARCHAR(64)",
                "`quota_used` INT", "`status` VARCHAR(16)", "`deleted` TINYINT");
        addIndex(conn, "enterprise_entitlement", "uk_company_ent", "`company_id`, `ent_code`, `deleted`");
        addColumns(conn, "commercial_order",
                "`order_no` VARCHAR(32)", "`idempotent_key` VARCHAR(64)", "`status` VARCHAR(24)");
        addIndex(conn, "commercial_order", "uk_order_no", "`order_no`");
        addIndex(conn, "commercial_order", "uk_order_idem", "`idempotent_key`");
        addColumns(conn, "advertising_slot",
                "`slot_code` VARCHAR(32)", "`status` VARCHAR(16)", "`deleted` TINYINT");
        addIndex(conn, "advertising_slot", "uk_slot_code", "`slot_code`, `deleted`");
        addColumns(conn, "advertising_campaign",
                "`company_id` BIGINT", "`slot_id` BIGINT", "`status` VARCHAR(24)");
    }

    // ========== DB-11: 完整历史库允许 baseline ==========
    @Test
    @DisplayName("DB-11: 完整历史库（核心+Phase13+Phase14表齐全）→ 允许 baseline")
    void testDb11_CompleteSchemaAllowsBaseline() throws Exception {
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "")) {
            createAllRequiredTables(conn);
            // 无 flyway_schema_history，非空库 → 应通过指纹校验
            assertDoesNotThrow(() -> FlywayBaselineGuard.validateDatabaseForBaseline(conn),
                    "完整 Schema 应允许 baseline");
        }
    }

    // ========== DB-12: 不完整库拒绝 baseline ==========
    @Test
    @DisplayName("DB-12: 缺少Phase13/14表的不完整库 → 拒绝 baseline")
    void testDb12_IncompleteSchemaRejectsBaseline() throws Exception {
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "")) {
            // 仅有核心表，缺少 Phase13/14 商业化表
            createTable(conn, "robot");
            createTable(conn, "brand");
            createTable(conn, "company");
            createTable(conn, "article");
            createTable(conn, "crawler_source");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> FlywayBaselineGuard.validateDatabaseForBaseline(conn),
                    "缺少 Phase13/14 表应拒绝 baseline");
            assertTrue(ex.getMessage().contains("缺少必需表"),
                    "错误信息应指明缺少表: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("enterprise_plan"),
                    "错误信息应列出缺失的 enterprise_plan: " + ex.getMessage());
        }
    }

    // ========== DB-12b: 仅有部分核心表也拒绝 ==========
    @Test
    @DisplayName("DB-12b: 仅有robot表（缺brand/company/article）→ 拒绝 baseline")
    void testDb12b_PartialCoreSchemaRejectsBaseline() throws Exception {
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "")) {
            createTable(conn, "robot");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> FlywayBaselineGuard.validateDatabaseForBaseline(conn));
            assertTrue(ex.getMessage().contains("brand"),
                    "应报告缺少 brand 表: " + ex.getMessage());
        }
    }

    // ========== DB-13: 空数据库拒绝 ==========
    @Test
    @DisplayName("DB-13: 空数据库 → 拒绝并要求先执行01-18初始化链")
    void testDb13_EmptyDatabaseRequiresExternalBootstrap() throws Exception {
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "")) {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> FlywayBaselineGuard.validateDatabaseForBaseline(conn),
                    "Server仅包含V19，空数据库必须先执行完整SQL初始化链");
            assertTrue(ex.getMessage().contains("01-18"),
                    "错误信息应给出01-18初始化要求: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("04_crawler_schema.sql"),
                    "错误信息应明确包含crawler schema: " + ex.getMessage());
            assertTrue(ex.getMessage().contains("04_migration_source_url.sql"),
                    "错误信息应明确包含source_url迁移: " + ex.getMessage());
        }
    }

    // ========== DB-14: 仅有 flyway_schema_history 的不完整库拒绝 ==========
    @Test
    @DisplayName("DB-14: 仅有flyway_schema_history的不完整库 → 拒绝升级")
    void testDb14_ExistingFlywayHistoryRejectsIncompleteSchema() throws Exception {
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "")) {
            // 只有 flyway_schema_history（模拟已迁移过），无业务表
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE `flyway_schema_history` (" +
                        "`installed_rank` INT PRIMARY KEY, `version` VARCHAR(50), " +
                        "`description` VARCHAR(200), `type` VARCHAR(20), `script` VARCHAR(1000), " +
                        "`checksum` INT, `installed_by` VARCHAR(100), " +
                        "`installed_on` TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "`execution_time` INT, `success` BOOLEAN)");
            }
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> FlywayBaselineGuard.validateDatabaseForBaseline(conn),
                    "Flyway历史不能让不完整Schema绕过指纹校验");
            assertTrue(ex.getMessage().contains("crawler_source"),
                    "错误信息应列出缺失的核心表: " + ex.getMessage());
        }
    }

    // ========== DB-15: 完整库+已有Flyway历史 → 允许（正常升级场景） ==========
    @Test
    @DisplayName("DB-15: 完整库+已有Flyway历史 → 允许增量迁移")
    void testDb15_CompleteSchemaWithFlywayHistory() throws Exception {
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "")) {
            createAllRequiredTables(conn);
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE `flyway_schema_history` (" +
                        "`installed_rank` INT PRIMARY KEY, `version` VARCHAR(50), " +
                        "`description` VARCHAR(200), `type` VARCHAR(20), `script` VARCHAR(1000), " +
                        "`checksum` INT, `installed_by` VARCHAR(100), " +
                        "`installed_on` TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "`execution_time` INT, `success` BOOLEAN)");
            }
            assertDoesNotThrow(() -> FlywayBaselineGuard.validateDatabaseForBaseline(conn));
        }
    }

    // ========== DB-16: 缺少crawler_source表 → 拒绝 baseline ==========
    @Test
    @DisplayName("DB-16: 缺少crawler_source表（采集系统核心表）→ 拒绝 baseline")
    void testDb16_MissingCrawlerSourceRejectsBaseline() throws Exception {
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "")) {
            // 有其他核心表+Phase13/14表，但缺少crawler_source
            for (String t : new String[]{"robot", "brand", "company", "article"}) {
                createTable(conn, t);
            }
            for (String t : new String[]{"company_claim", "company_profile_change"}) {
                createTable(conn, t);
            }
            for (String t : new String[]{"enterprise_plan", "enterprise_subscription",
                    "enterprise_entitlement", "commercial_order", "advertising_slot", "advertising_campaign"}) {
                createTable(conn, t);
            }
            // 不创建 crawler_source

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> FlywayBaselineGuard.validateDatabaseForBaseline(conn),
                    "缺少 crawler_source 应拒绝 baseline");
            assertTrue(ex.getMessage().contains("crawler_source"),
                    "错误信息应列出缺失的 crawler_source: " + ex.getMessage());
        }
    }

    @Test
    @DisplayName("DB-17: 表名齐全但缺少关键列 → 拒绝baseline")
    void testDb17_MissingCriticalColumnRejectsBaseline() throws Exception {
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "");
             Statement st = conn.createStatement()) {
            createAllRequiredTables(conn);
            st.execute("ALTER TABLE `company_claim` DROP COLUMN `proof_materials`");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> FlywayBaselineGuard.validateDatabaseForBaseline(conn));
            assertTrue(ex.getMessage().contains("company_claim.proof_materials"),
                    "应报告缺少私有材料归属列: " + ex.getMessage());
        }
    }

    @Test
    @DisplayName("DB-18: 表列齐全但缺少订单幂等唯一索引 → 拒绝baseline")
    void testDb18_MissingCriticalIndexRejectsBaseline() throws Exception {
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "");
             Statement st = conn.createStatement()) {
            createAllRequiredTables(conn);
            st.execute("DROP INDEX `uk_order_idem`");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> FlywayBaselineGuard.validateDatabaseForBaseline(conn));
            assertTrue(ex.getMessage().contains("commercial_order.uk_order_idem"),
                    "应报告缺少订单幂等唯一索引: " + ex.getMessage());
        }
    }
}
