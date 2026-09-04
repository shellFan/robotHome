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
        log.info("H2 测试库初始化完成（schema + init + demo）");
    }

    private void run(String sql) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            // 逐条执行，便于定位语法问题；切分需忽略字符串字面量中的分号（教程正文含代码片段）
            for (String stmt : splitStatements(sql)) {
                String trimmed = stmt.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                st.execute(trimmed);
            }
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
