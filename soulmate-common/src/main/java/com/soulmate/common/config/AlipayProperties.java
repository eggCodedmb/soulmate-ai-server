package com.soulmate.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "soulmate.alipay")
public class AlipayProperties {

    /** 应用ID */
    private String appId = "";

    /** 应用私钥 */
    private String privateKey = "";

    /** 支付宝公钥 */
    private String alipayPublicKey = "";

    /** 网关地址（沙箱环境） */
    private String gateway = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    /** 异步通知地址 */
    private String notifyUrl = "http://localhost:8080/api/payment/notify";

    /** 同步跳转地址 */
    private String returnUrl = "http://localhost:8080/api/payment/return";

    /** 签名算法 */
    private String signType = "RSA2";

    /** 编码 */
    private String charset = "UTF-8";

    /** 返回格式 */
    private String format = "json";
}
