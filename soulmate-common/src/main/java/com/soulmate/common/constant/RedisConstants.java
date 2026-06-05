package com.soulmate.common.constant;

/**
 * Redis Key 常量
 */
public final class RedisConstants {

    private RedisConstants() {}

    /** 用户会话: user:session:{token} */
    public static final String USER_SESSION = "user:session:";

    /** 每日消息计数: user:daily_msg:{userId}:{date} */
    public static final String USER_DAILY_MSG = "user:daily_msg:";

    /** 邮箱验证码: verify:code:{email} */
    public static final String VERIFY_CODE = "verify:code:";

    /** 验证码发送频率: verify:rate:{email} */
    public static final String VERIFY_CODE_RATE = "verify:rate:";

    /** 对话上下文: companion:context:{conversationId} */
    public static final String COMPANION_CONTEXT = "companion:context:";

    /** AI正在输入: companion:typing:{conversationId} */
    public static final String COMPANION_TYPING = "companion:typing:";

    /** 用户订阅缓存: user:subscription:{userId} */
    public static final String USER_SUBSCRIPTION = "user:subscription:";

    /** 当前模型配置: config:model:current */
    public static final String CONFIG_MODEL_CURRENT = "config:model:current";

    /** 验证码有效期（分钟） */
    public static final long VERIFY_CODE_TTL_MINUTES = 5;

    /** 验证码发送间隔（秒） */
    public static final long VERIFY_CODE_RATE_SECONDS = 60;
}
