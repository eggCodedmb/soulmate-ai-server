package com.soulmate.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 和风天气配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "soulmate.qweather")
public class QweatherProperties {

    /** API Key */
    private String apiKey;

    /** API 自定义域名（和风天气新版统一入口） */
    private String apiHostUrl = "https://devapi.qweather.com";
}
