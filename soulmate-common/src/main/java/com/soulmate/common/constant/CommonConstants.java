package com.soulmate.common.constant;

/**
 * 通用常量
 */
public final class CommonConstants {

    private CommonConstants() {}

    /** 请求头中的 Token 名 */
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 请求上下文中的用户ID Key */
    public static final String CURRENT_USER_ID = "currentUserId";

    /** 逻辑删除：未删除 */
    public static final int NOT_DELETED = 0;
    /** 逻辑删除：已删除 */
    public static final int DELETED = 1;

    /** 状态：禁用 */
    public static final int STATUS_DISABLED = 0;
    /** 状态：正常 */
    public static final int STATUS_NORMAL = 1;

    /** 分页默认值 */
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
}
