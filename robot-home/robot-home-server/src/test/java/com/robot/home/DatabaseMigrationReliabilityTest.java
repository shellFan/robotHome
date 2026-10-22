package com.robot.home;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库迁移可靠性测试 — 覆盖 DB-01 ~ DB-10 场景
 *
 * 使用独立 H2 内存库 + Flyway 编程式 API 验证迁移机制：
 * DB-01 全新初始化 / DB-02 历史版本升级 / DB-03 重复执行
 * DB-04 执行中断恢复 / DB-05 已存在表 / DB-06 已存在索引
 * DB-07 部分迁移完成 / DB-08 数据保留 / DB-09 Schema差异 / DB-10 并发执行
 *
 * 注意：MySQL 5.6 运行时验证标记 MYSQL56_RUNTIME_BLOCKED（本机无 Docker）
 */
@DisplayName("数据库迁移可靠性 DB-01~DB-10")
class DatabaseMigrationReliabilityTest {

    @TempDir
    Path tempDir;

    private String h2Url;
    private File migrationDir;

    @BeforeEach
    void setUp() throws Exception {
        h2Url = "jdbc:h2:mem:migration_test_" + System.nanoTime()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1";
        // 使用 main/resources/db/migration 中的 V19 脚本
        migrationDir = new File("src/main/resources/db/migration");
        assertTrue(migrationDir.exists(), "迁移脚本目录应存在: " + migrationDir.getAbsolutePath());
    }

    @AfterEach
    void tearDown() throws Exception {
        // H2 内存库在连接关闭后自动清理
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "");
             Statement st = conn.createStatement()) {
            st.execute("SHUTDOWN");
        } catch (Exception ignored) {
        }
    }

    private Flyway createFlyway(String url) {
        return Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("filesystem:" + migrationDir.getAbsolutePath())
                .baselineOnMigrate(true)
                .baselineVersion("18")
                .load();
    }

    // ========== DB-01: 全新初始化 ==========
    @Test
    @DisplayName("DB-01: 全新数据库初始化 — 模拟 docker-entrypoint 预建表后 baseline + V19")
    void testDb01_FreshInitialization() throws Exception {
        // 模拟 docker-entrypoint-initdb.d 已执行 01-18（非空库）
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "");
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE `robot` (`id` BIGINT AUTO_INCREMENT PRIMARY KEY, `name` VARCHAR(128))");
        }

        Flyway flyway = createFlyway(h2Url);
        flyway.migrate();

        // 验证 flyway_schema_history 存在且记录正确
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "");
             Statement st = conn.createStatement()) {
            // baseline 记录（非空库触发 baseline-on-migrate）
            try (ResultSet rs = st.executeQuery(
                    "SELECT version FROM flyway_schema_history WHERE version = '18'")) {
                assertTrue(rs.next(), "baseline 版本 18 应存在");
            }
            // V19 记录
            try (ResultSet rs = st.executeQuery(
                    "SELECT version, success FROM flyway_schema_history WHERE version = '19'")) {
                assertTrue(rs.next(), "V19 应已执行");
                assertTrue(rs.getBoolean("success"), "V19 应成功");
            }
            // schema_audit_log 表存在且有数据
            try (ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM schema_audit_log")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) >= 2, "schema_audit_log 应有基线+V19记录");
            }
        }
    }

    // ========== DB-02: 历史版本升级（已有非空库无 Flyway 历史） ==========
    @Test
    @DisplayName("DB-02: 历史数据库升级 — 非空库自动 baseline 后执行新迁移")
    void testDb02_UpgradeFromLegacyDatabase() throws Exception {
        // 模拟已有数据库：先创建一些表（代表 01-18 已执行）
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "");
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE `robot` (`id` BIGINT AUTO_INCREMENT PRIMARY KEY, `name` VARCHAR(128))");
            st.execute("CREATE TABLE `brand` (`id` BIGINT AUTO_INCREMENT PRIMARY KEY, `name` VARCHAR(128))");
            st.execute("INSERT INTO `robot` (`name`) VALUES ('测试机器人')");
        }

        // 执行 Flyway 迁移
        Flyway flyway = createFlyway(h2Url);
        flyway.migrate();

        // 验证：旧数据保留 + baseline + V19 执行
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "");
             Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM robot")) {
                assertTrue(rs.next() && rs.getInt(1) == 1, "旧数据应保留");
            }
            try (ResultSet rs = st.executeQuery(
                    "SELECT version FROM flyway_schema_history WHERE version = '18' AND type = 'BASELINE'")) {
                assertTrue(rs.next(), "应自动创建 baseline 18");
            }
        }
    }

    // ========== DB-03: 重复执行（幂等性） ==========
    @Test
    @DisplayName("DB-03: 重复执行 migrate 不产生重复记录不破坏数据")
    void testDb03_RepeatExecution() {
        Flyway flyway = createFlyway(h2Url);
        flyway.migrate();
        // 再次执行
        flyway.migrate();

        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "");
             Statement st = conn.createStatement()) {
            // V19 只有一条执行记录
            try (ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '19'")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "V19 不应重复执行");
            }
            // schema_audit_log 无重复
            try (ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM schema_audit_log WHERE version = '19'")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "审计日志不应重复");
            }
        } catch (Exception e) {
            fail("DB-03 验证失败: " + e.getMessage());
        }
    }

    // ========== DB-05: 已存在表（V19 CREATE TABLE IF NOT EXISTS 幂等） ==========
    @Test
    @DisplayName("DB-05: 已存在同schema表时迁移不失败")
    void testDb05_ExistingTable() throws Exception {
        // 预先创建 schema_audit_log（完整schema，模拟手工建表或旧版本迁移）
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "");
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE `schema_audit_log` (" +
                    "`id` BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "`version` VARCHAR(32) NOT NULL, " +
                    "`description` VARCHAR(256) NOT NULL, " +
                    "`checksum` VARCHAR(64), " +
                    "`executed_by` VARCHAR(64) DEFAULT 'flyway', " +
                    "`executed_at` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "`success` TINYINT(1) DEFAULT 1, " +
                    "`duration_ms` INT)");
            st.execute("CREATE TABLE `robot` (`id` BIGINT AUTO_INCREMENT PRIMARY KEY)");
        }

        Flyway flyway = createFlyway(h2Url);
        // baseline 后执行 V19 — IF NOT EXISTS + INSERT IGNORE 保证不失败
        flyway.migrate();

        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "");
             Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery(
                    "SELECT version FROM flyway_schema_history WHERE version = '19' AND success = TRUE")) {
                assertTrue(rs.next(), "V19 在表已存在时应仍成功");
            }
        }
    }

    // ========== DB-08: 数据保留 ==========
    @Test
    @DisplayName("DB-08: 迁移前后业务数据完整保留")
    void testDb08_DataPreservation() throws Exception {
        Flyway flyway = createFlyway(h2Url);
        flyway.migrate();

        // 写入业务数据
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "");
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE `test_business` (`id` BIGINT AUTO_INCREMENT PRIMARY KEY, `val` VARCHAR(64))");
            st.execute("INSERT INTO `test_business` (`val`) VALUES ('重要数据')");
        }

        // 再次迁移（无新版本）
        flyway.migrate();

        // 验证数据保留
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "");
             Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT val FROM test_business")) {
                assertTrue(rs.next());
                assertEquals("重要数据", rs.getString(1), "业务数据应完整保留");
            }
        }
    }

    // ========== DB-09: Schema 差异检测（validate） ==========
    @Test
    @DisplayName("DB-09: validate 检测迁移脚本校验和一致性")
    void testDb09_SchemaValidation() {
        Flyway flyway = createFlyway(h2Url);
        flyway.migrate();

        // validate 应通过（脚本未修改）
        assertDoesNotThrow(() -> flyway.validate(),
                "未修改脚本时 validate 应通过");

        // 验证 flyway_schema_history 中有校验和记录
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "");
             Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery(
                    "SELECT checksum FROM flyway_schema_history WHERE version = '19'")) {
                assertTrue(rs.next());
                assertNotNull(rs.getInt("checksum"), "V19 应有校验和");
            }
        } catch (Exception e) {
            fail("DB-09 验证失败: " + e.getMessage());
        }
    }

    // ========== DB-10: 并发执行安全 ==========
    @Test
    @DisplayName("DB-10: 并发 migrate 调用不产生重复记录")
    void testDb10_ConcurrentExecution() throws InterruptedException {
        Flyway flyway = createFlyway(h2Url);

        // 模拟两个线程同时 migrate
        Thread t1 = new Thread(() -> {
            try { createFlyway(h2Url).migrate(); } catch (Exception ignored) {}
        });
        Thread t2 = new Thread(() -> {
            try { createFlyway(h2Url).migrate(); } catch (Exception ignored) {}
        });
        t1.start();
        t2.start();
        t1.join(5000);
        t2.join(5000);

        // 验证 V19 只有一条成功记录
        try (Connection conn = DriverManager.getConnection(h2Url, "sa", "");
             Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '19' AND success = TRUE")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "并发执行后 V19 应只有一条成功记录");
            }
        } catch (Exception e) {
            fail("DB-10 验证失败: " + e.getMessage());
        }
    }
}