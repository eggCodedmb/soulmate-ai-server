package com.soulmate.common.util;

/**
 * 雪花ID ↔ PgVector UUID 格式互转工具
 * <p>
 * PgVectorStore 要求 Document ID 为 UUID 格式，
 * 而业务层使用雪花算法 Long ID，需要可逆转换。
 */
public final class VectorIdUtil {

    private VectorIdUtil() {
    }

    /**
     * 雪花ID → UUID格式字符串
     * <p>
     * 格式: 00000000-0000-0000-{high}-{low}，高32位存Long高32位，低32位存Long低32位
     */
    public static String toVectorId(Long id) {
        long val = id;
        long high = (val >>> 32) & 0xFFFFFFFFL;
        long low = val & 0xFFFFFFFFL;
        return String.format("00000000-0000-0000-%04x-%08x", high, low);
    }

    /**
     * UUID格式字符串 → 雪花ID
     */
    public static Long fromVectorId(String uuid) {
        String hex = uuid.replace("-", "");
        long high = Long.parseLong(hex.substring(8, 16), 16);
        long low = Long.parseLong(hex.substring(16, 24), 16);
        return (high << 32) | low;
    }
}
