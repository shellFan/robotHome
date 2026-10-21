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
 * DB-13 空数据库 → 允许全新初始化
 * DB-14 已有 flyway_schema_history → 允许增量迁移
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

    /** 创建全部必需表（核心+Phase13+Phase14） */
    private void createAllRequiredTables(Connection conn) throws Exception {
        // 核心
        for (String t : new String[]{"robot", "brand", "company", "article", "crawler_source"}) {
            createTable(conn, t);
        }
        // Phase13
        for (String t : new String[]{"enterprise_company", "procurement_lead"}) {
            createTable(conn, t);
        }
        // Phase14
        for (String t : new String[]{"enterprise_plan", "enterprise_subscription",
                "enterprise_entitlement", "enterprise_order", "advertising_slot", "advertising_campaign"}) {
            createTable(conn, t);
        }
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

    // ========== DB-13: 空数据库允许 ==========
    @Test
    @DisplayName("DB-13: 空数据库 → 允许全新初始化")
    void testDb13_EmptyDatabaseAllowed() throws Exception {
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "")) {
            // 空库，无任何表
            assertDoesNotThrow(() -> FlywayBaselineGuard.validateDatabaseForBaseline(conn),
                    "空数据库应允许全新初始化");
        }
    }

    // ========== DB-14: 已有 flyway_schema_history 允许 ==========
    @Test
    @DisplayName("DB-14: 已有 flyway_schema_history → 允许增量迁移（跳过指纹校验）")
    void testDb14_ExistingFlywayHistoryAllowed() throws Exception {
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
            // 有 flyway_schema_history → 跳过指纹校验，允许增量迁移
            assertDoesNotThrow(() -> FlywayBaselineGuard.validateDatabaseForBaseline(conn),
                    "已有 Flyway 历史应允许增量迁移");
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
            for (String t : new String[]{"enterprise_company", "procurement_lead"}) {
                createTable(conn, t);
            }
            for (String t : new String[]{"enterprise_plan", "enterprise_subscription",
                    "enterprise_entitlement", "enterprise_order", "advertising_slot", "advertising_campaign"}) {
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
}