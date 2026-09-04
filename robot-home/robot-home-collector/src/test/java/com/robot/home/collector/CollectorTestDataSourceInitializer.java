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
 * 复用项目 sql/ 目录下的生产脚本（通过 SQL 翻译适配 H2）
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
        // 主站 schema（article, robot, robot_video, robot_param_def, robot_param_value 等）
        run(CollectorTestSqlSupport.load("01_schema.sql"));
        // 采集器 schema（crawler_article, crawler_product, crawler_media 等）
        run(CollectorTestSqlSupport.load("04_crawler_schema.sql"));
        // 初始化数据
        run(CollectorTestSqlSupport.load("02_init_data.sql"));
        log.info("H2 测试库初始化完成（主站schema + 采集器schema + 初始化数据）");
    }

    private void run(String sql) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            for (String stmt : splitStatements(sql)) {
                String trimmed = stmt.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                st.execute(trimmed);
            }
        }
    }

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