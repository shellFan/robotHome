package com.robot.home.collector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 机器人之家 - 数据采集模块启动类
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
        "com.robot.home.collector",
        "com.robot.home.common"
})
public class CollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollectorApplication.class, args);
    }
}