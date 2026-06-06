package com.soulmate;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SoulMate AI 启动入口
 */
@SpringBootApplication
@MapperScan("com.soulmate.mapper")
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class SoulMateApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoulMateApplication.class, args);
    }
}
