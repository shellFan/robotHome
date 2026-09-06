package com.robot.home.collector.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 数据库Schema自动初始化（仅生产环境）
 * <p>
 * ⚠️ @Deprecated: 此类将在后续版本中移除。
 * 数据库Schema管理应统一由 Flyway 负责（V1 基线 + 增量迁移）。
 * 新部署应使用 Flyway 迁移，不再依赖此初始化器。
 * <p>
 * 职责：
 * - 首次启动时创建采集系统所需的表（crawler_source 不存在时）
 * - 增量迁移由 Flyway 管理，本类不再执行
 * <p>
 * ⚠️ 测试环境通过 @Profile("!test") 禁用，测试使用 CollectorTestDataSourceInitializer
 * ⚠️ 建表失败立即终止，不吞异常
 * ⚠️ 不再执行 DELETE FROM crawler_url 或清 Redis 缓存（这些是破坏性操作）
 */
@Deprecated
@Component
@Profile("!test")
public class SchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 应用启动后检查并创建采集系统所需的表
     * 仅在 crawler_source 表不存在时执行建表脚本
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initSchema() {
        log.info("检查采集系统数据库表...");
        try {
            // 检查crawler_source表是否存在
            List<String> tables = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crawler_source'",
                    String.class);

            if (tables.isEmpty()) {
                log.info("采集系统表不存在，开始自动创建...");
                executeSchemaSql();
                log.info("采集系统表创建完成");
            } else {
                log.info("采集系统表已存在，跳过创建（增量迁移由 Flyway 管理）");
            }
        } catch (Exception e) {
            // 建表失败必须立即终止，不允许在表缺失的情况下继续运行
            throw new RuntimeException("采集系统表初始化失败，应用无法启动: " + e.getMessage(), e);
        }
    }

    /**
     * 执行建表脚本
     * ⚠️ 建表失败立即抛异常终止，不吞异常
     */
    private void executeSchemaSql() {
        try {
            ClassPathResource resource = new ClassPathResource("schema/crawler_schema.sql");
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            String[] statements = sql.split(";");
            for (String stmt : statements) {
                String trimmed = stmt.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.toUpperCase().startsWith("USE ")) continue;
                if (trimmed.toUpperCase().startsWith("DROP TABLE")) continue;

                // 将CREATE TABLE替换为CREATE TABLE IF NOT EXISTS
                if (trimmed.toUpperCase().startsWith("CREATE TABLE")) {
                    trimmed = trimmed.replaceFirst("(?i)CREATE TABLE", "CREATE TABLE IF NOT EXISTS");
                }

                try {
                    jdbcTemplate.execute(trimmed);
                } catch (Exception e) {
                    // 建表失败必须立即终止
                    String preview = trimmed.substring(0, Math.min(200, trimmed.length()));
                    throw new RuntimeException("执行建表SQL失败: " + e.getMessage()
                            + "\n  SQL: " + preview + "...", e);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("读取Schema SQL文件失败: " + e.getMessage(), e);
        }
    }
}