package com.robot.home.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 采集器集成测试：在 H2 中执行建表脚本与初始化数据
 * 复用项目 sql/ 目录下的生产脚本（通过 CollectorTestSqlSupport 翻译适配 H2）
 * <p>
 * 加载顺序：
 * 1. 01_schema.sql — 主站表（article, robot, brand, company, category 等）
 * 2. 04_crawler_schema.sql — 采集器表（crawler_source, crawler_article, crawler_product 等）
 * 3. 05_brand_alias_and_indexes.sql — Phase 2 新增字段+新表（brand_alias, param_mapping, product_change）
 * 4. 02_init_data.sql — 基础数据（分类、字典、角色等）
 * <p>
 * ⚠️ 建表失败立即终止，不吞异常
 */
@TestConfiguration
public class CollectorTestDataSourceInitializer {

    private static final Logger log = LoggerFactory.getLogger(CollectorTestDataSourceInitializer.class);

    private final DataSource dataSource;

    public CollectorTestDataSourceInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() throws Exception {
        log.info("开始初始化 H2 测试库...");

        // 主站 schema（article, robot, robot_video, robot_param_def, robot_param_value 等）
        run(CollectorTestSqlSupport.load("01_schema.sql"));
        log.info("H2 测试库：主站 schema 加载完成");

        // 采集器 schema（crawler_article, crawler_product, crawler_media 等）
        run(CollectorTestSqlSupport.load("04_crawler_schema.sql"));
        log.info("H2 测试库：采集器 schema 加载完成");

        // Phase 2 迁移：新增字段（brand/company/robot）+ 新表（brand_alias, param_mapping, product_change）
        run(CollectorTestSqlSupport.load("05_brand_alias_and_indexes.sql"));
        log.info("H2 测试库：Phase 2 迁移加载完成");

        // 初始化数据（分类、字典、角色等基础数据）
        run(CollectorTestSqlSupport.load("02_init_data.sql"));
        log.info("H2 测试库：初始化数据加载完成");

        log.info("H2 测试库初始化完成（主站schema + 采集器schema + Phase2迁移 + 初始化数据）");
    }

    /**
     * 执行 SQL 脚本，采用严格白名单错误处理策略：
     * - CREATE 失败：立即终止（建表是基础，不可跳过）
     * - ALTER TABLE 列/索引已存在：跳过（幂等迁移场景）
     * - INSERT 重复主键/唯一约束：跳过（幂等初始化数据场景）
     * - 其他所有错误：立即终止（不吞异常，避免隐藏问题）
     */
    private void run(String sql) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            for (String stmt : splitStatements(sql)) {
                String trimmed = stmt.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    st.execute(trimmed);
                } catch (Exception e) {
                    String msg = e.getMessage();
                    String upper = trimmed.toUpperCase();

                    // === 白名单：可容忍的错误 ===

                    // 1. ALTER TABLE ADD COLUMN 列已存在（H2 IF NOT EXISTS 兼容）
                    if (upper.startsWith("ALTER") && msg != null
                            && (msg.contains("Duplicate column") || msg.contains("Column exists"))) {
                        log.debug("跳过已存在列: {}", msg);
                        continue;
                    }
                    // 2. ALTER TABLE ADD INDEX/CONSTRAINT 索引/约束已存在
                    if (upper.startsWith("ALTER") && msg != null
                            && (msg.contains("already exists") || msg.contains("Index exists")
                                || msg.contains("Constraint already exists"))) {
                        log.debug("跳过已存在索引/约束: {}", msg);
                        continue;
                    }
                    // 3. CREATE TABLE IF NOT EXISTS 表已存在（translate已添加IF NOT EXISTS）
                    if (upper.startsWith("CREATE") && msg != null
                            && msg.contains("already exists")) {
                        log.debug("跳过已存在表: {}", msg);
                        continue;
                    }
                    // 4. INSERT 重复数据（Duplicate key / unique constraint / PRIMARY KEY）
                    if (msg != null && (msg.contains("Duplicate key") || msg.contains("unique constraint")
                            || msg.contains("PRIMARY KEY"))) {
                        log.debug("跳过重复数据: {}", msg);
                        continue;
                    }

                    // === 非白名单错误：必须终止 ===
                    String preview = trimmed.substring(0, Math.min(200, trimmed.length()));
                    throw new RuntimeException("H2 SQL 执行失败（非白名单错误）: " + msg
                            + "\n  SQL: " + preview + "...", e);
                }
            }
        }
    }

    /**
     * 按分号拆分 SQL 语句，正确处理字符串内的分号
     */
    private static List<String> splitStatements(String sql) {
        List<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (inString) {
                sb.append(c);
                if (c == '\\') {
                    if (i + 1 < sql.length()) {
                        sb.append(sql.charAt(++i));
                    }
                } else if (c == '\'') {
                    inString = false;
                }
                continue;
            }
            if (c == '\'') {
                inString = true;
                sb.append(c);
            } else if (c == ';') {
                list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            list.add(sb.toString());
        }
        return list;
    }
}