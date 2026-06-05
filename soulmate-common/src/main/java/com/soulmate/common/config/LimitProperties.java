package com.soulmate.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 免费用户配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "soulmate.limits")
public class LimitProperties {

    /** 免费用户每日消息上限 */
    private int freeDailyMessages = 30;

    /** 免费用户最大伴侣数 */
    private int freeMaxCompanions = 1;
}
