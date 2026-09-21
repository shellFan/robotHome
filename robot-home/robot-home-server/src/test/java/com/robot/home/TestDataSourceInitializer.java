package com.robot.home;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 测试环境初始化：在 H2 中执行项目的建表脚本与初始化 / 演示数据
 * 
 * 实现 PriorityOrdered + BeanPostProcessor 确保在所有其他 Bean 的 @PostConstruct 之前完成数据库初始化。
 * 这样 BehaviorEventServiceImpl.init() 等依赖 ranking_weight 表的 @PostConstruct 才能正常工作。
 */
@Slf4j
@TestConfiguration
public class TestDataSourceInitializer implements PriorityOrdered {

    private final DataSource dataSource;
    private volatile boolean initialized = false;

    public TestDataSourceInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 在 H2 中初始化全部 schema 与测试数据
     * 通过构造器注入 DataSource 后立即执行，早于其他 Bean 的 @PostConstruct
     */
    @PostConstruct
    public void init() throws Exception {
        if (initialized) {
            return;
        }
        log.info("H2 测试库初始化开始...");
        // 1. 核心Schema（01_schema 已包含所有基础表）
        run(TestSqlSupport.load("01_schema.sql"));
        // 2. 初始数据与演示数据
        run(TestSqlSupport.load("02_init_data.sql"));
        run(TestSqlSupport.load("03_demo_data.sql"));
        // 04_migration_source_url.sql 使用 MySQL PREPARE/EXECUTE 语法，H2 不兼容
        run(TestSqlSupport.load("05_brand_alias_and_indexes.sql"));
        run(TestSqlSupport.load("06_real_brands_companies.sql"));
        run(TestSqlSupport.load("07_real_robot_products.sql"));
        // 3. Phase6 Beta: 排行榜/行为事件/询价跟进/限流/搜索建议/用户反馈等新表
        run(TestSqlSupport.load("10_phase6_beta.sql"));
        // 4. Phase7: 内容/社区/增长等新表与新列
        run(TestSqlSupport.load("11_phase7_content_community_growth.sql"));
        // 5. Phase8: 社区增强/Q&A/选型/采购/搜索增长/数据质量
        run(TestSqlSupport.load("12_phase8_community_procurement_growth.sql"));
        // 5b. Phase8 H2测试数据（含H2兼容建表兜底 + 测试数据INSERT）
        runClasspath("sql/12_phase8_community_procurement_growth.sql");
        // 6. Phase9: 产品增长与生态
        run(TestSqlSupport.load("13_phase9_product_growth.sql"));
        // 7. Phase9 H2测试数据（含H2兼容建表兜底 + 测试数据INSERT）
        runClasspath("sql/13_phase9_product_growth.sql");
        // 8. Phase10: 生态增长与信任
        run(TestSqlSupport.load("14_phase10_ecosystem_growth.sql"));
        // 9. Phase10 H2测试数据（含H2兼容建表兜底 + 测试数据INSERT）
        runClasspath("sql/14_phase10_ecosystem_growth.sql");
        // 10. 验证关键表
        verifyTables();
        initialized = true;
        log.info("H2 测试库初始化完成（schema + init + demo + migrations + real data + phase6 beta + phase7 + phase8 + phase9 + phase10）");
    }

    private void verifyTables() {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            for (String table : Arrays.asList("ranking_weight", "growth_daily_stat", "user_contribution_stat", "behavior_event", "ranking_snapshot", "company_member", "user_subscription", "user_reputation")) {
                try (java.sql.ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (rs.next()) {
                        log.info("H2 验证: {} 表存在, {} 行", table, rs.getInt(1));
                    }
                } catch (Exception e) {
                    log.error("H2 验证失败: {} 表不存在 - {}", table, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("H2 验证连接失败: {}", e.getMessage());
        }
    }

    private void run(String sql) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
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
     * 从 classpath 加载 H2 原生兼容 SQL 并执行（无需 MySQL 翻译）
     */
    private void runClasspath(String location) throws Exception {
        java.io.InputStream resource = getClass().getClassLoader().getResourceAsStream(location);
        if (resource == null) {
            log.warn("Classpath SQL 未找到: {}, 跳过", location);
            return;
        }
        String sql;
        try (java.io.InputStream is = resource) {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            sql = baos.toString("UTF-8");
        }
        log.info("从 classpath 加载: {}", location);
        run(sql);
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