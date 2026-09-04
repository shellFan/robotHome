package com.robot.home.collector.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

/**
 * 数据库Schema自动初始化
 * 应用启动后检查并创建采集系统所需的表
 */
@Component
public class SchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public SchemaInitializer(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
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
                log.info("采集系统表已存在，跳过创建");
                // 执行增量迁移
                runMigrations();
            }
        } catch (Exception e) {
            log.error("检查/创建采集系统表失败: {}", e.getMessage(), e);
        }
    }

    private void runMigrations() {
        try {
            // 迁移1: crawler_article.title 允许NULL
            try {
                jdbcTemplate.execute("ALTER TABLE crawler_article MODIFY COLUMN `title` VARCHAR(512) DEFAULT NULL COMMENT '标题'");
                log.info("迁移成功: crawler_article.title 允许NULL");
            } catch (Exception e) {
                // 如果已经是NULL允许的，会报错，忽略
                log.debug("迁移跳过(crawler_article.title): {}", e.getMessage());
            }

            // 迁移2: 清除旧的去重数据（修复之前markFetched默认标记SUCCESS导致的错误状态）
            try {
                int deleted = jdbcTemplate.update("DELETE FROM crawler_url");
                log.info("迁移: 清除旧URL去重数据, 删除{}条记录", deleted);
            } catch (Exception e) {
                log.debug("迁移跳过(清除crawler_url): {}", e.getMessage());
            }

            // 迁移3: 清除Redis中的URL去重缓存
            try {
                if (redisTemplate != null) {
                    Set<String> keys = redisTemplate.keys("crawler:url:*");
                    if (keys != null && !keys.isEmpty()) {
                        redisTemplate.delete(keys);
                        log.info("迁移: 清除Redis URL去重缓存, 删除{}个key", keys.size());
                    } else {
                        log.info("迁移: Redis无URL去重缓存需要清除");
                    }
                }
            } catch (Exception e) {
                log.debug("迁移跳过(清除Redis缓存): {}", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("增量迁移执行异常: {}", e.getMessage());
        }
    }

    private void executeSchemaSql() {
        try {
            ClassPathResource resource = new ClassPathResource("schema/crawler_schema.sql");
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            // 移除USE语句和DROP TABLE语句，改为CREATE TABLE IF NOT EXISTS
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

                // 跳过INSERT语句中的初始化数据（可选，第一次创建时也执行）
                try {
                    jdbcTemplate.execute(trimmed);
                } catch (Exception e) {
                    log.warn("执行SQL语句失败(继续): {}", e.getMessage());
                    log.debug("失败SQL: {}", trimmed.substring(0, Math.min(200, trimmed.length())));
                }
            }
        } catch (Exception e) {
            log.error("读取Schema SQL文件失败: {}", e.getMessage(), e);
        }
    }
}