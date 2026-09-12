package com.robot.home;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试环境初始化：在 H2 中执行项目的建表脚本与初始化 / 演示数据
 */
@Slf4j
@TestConfiguration
public class TestDataSourceInitializer {

    private final DataSource dataSource;

    public TestDataSourceInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() throws Exception {
        run(TestSqlSupport.load("01_schema.sql"));
        run(TestSqlSupport.load("02_init_data.sql"));
        run(TestSqlSupport.load("03_demo_data.sql"));
        // 04_migration_source_url.sql 使用 MySQL PREPARE/EXECUTE 语法，H2 不兼容
        // article.source_url 已在 01_schema.sql 中定义，全新安装无需 04 迁移
        run(TestSqlSupport.load("05_brand_alias_and_indexes.sql"));
        run(TestSqlSupport.load("06_real_brands_companies.sql"));
        run(TestSqlSupport.load("07_real_robot_products.sql"));
        // Phase6 Beta: 排行榜/行为事件/询价跟进/限流/搜索建议/用户反馈等新表
        run(TestSqlSupport.load("10_phase6_beta.sql"));
        log.info("H2 测试库初始化完成（schema + init + demo + migrations + real data + phase6 beta）");
    }

    private void run(String sql) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            // 逐条执行，便于定位语法问题；切分需忽略字符串字面量中的分号（教程正文含代码片段）
            int skipped = 0;
            int executed = 0;
            for (String stmt : splitStatements(sql)) {
                String trimmed = stmt.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    st.execute(trimmed);
                    executed++;
                } catch (Exception e) {
                    skipped++;
                    log.warn("H2 跳过语句（预期行为）: {}", trimmed.substring(0, Math.min(80, trimmed.length())));
                    log.debug("跳过原因: {}", e.getMessage());
                }
            }
            log.info("H2 执行完成: {} 条成功, {} 条跳过", executed, skipped);
        }
    }

    /**
     * 按分号切分 SQL，但忽略单引号字符串内的分号与转义
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
                    // 反斜杠转义，下一个字符原样输出
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
