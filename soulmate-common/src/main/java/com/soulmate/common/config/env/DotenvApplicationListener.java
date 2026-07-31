package com.soulmate.common.config.env;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 开发模式下从 .env 文件自动加载环境变量到 Spring Environment
 * 使用 ApplicationListener<ApplicationEnvironmentPreparedEvent> 以兼容 Spring Boot 4.0+
 */
public class DotenvApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "dotenvProperties";

    @Override
    public int getOrder() {
        // 在常规配置解析之前运行
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        File envFile = findDotEnvFile();
        if (envFile == null || !envFile.exists()) {
            return;
        }

        Map<String, Object> dotenvMap = parseDotEnv(envFile);
        if (!dotenvMap.isEmpty()) {
            MutablePropertySources propertySources = environment.getPropertySources();
            propertySources.addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, dotenvMap));
        }
    }

    private File findDotEnvFile() {
        // 1. 本地优先的 .env.local 文件
        File localEnv = new File(".env.local");
        if (localEnv.exists() && localEnv.isFile()) {
            return localEnv;
        }
        File parentLocalEnv = new File("../.env.local");
        if (parentLocalEnv.exists() && parentLocalEnv.isFile()) {
            return parentLocalEnv;
        }

        // 2. 标准 .env 文件
        File currentDirEnv = new File(".env");
        if (currentDirEnv.exists() && currentDirEnv.isFile()) {
            return currentDirEnv;
        }
        File parentDirEnv = new File("../.env");
        if (parentDirEnv.exists() && parentDirEnv.isFile()) {
            return parentDirEnv;
        }

        return null;
    }

    private Map<String, Object> parseDotEnv(File file) {
        Map<String, Object> map = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // 忽略空行和注释
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIndex = line.indexOf('=');
                if (eqIndex > 0) {
                    String key = line.substring(0, eqIndex).trim();
                    String value = line.substring(eqIndex + 1).trim();

                    // 去掉包围的引号
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        if (value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        }
                    }

                    if (!key.isEmpty()) {
                        map.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DotenvApplicationListener] Warning: Failed to parse .env file: " + e.getMessage());
        }
        return map;
    }
}
