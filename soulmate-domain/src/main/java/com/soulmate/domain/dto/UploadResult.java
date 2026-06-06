package com.soulmate.domain.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件上传结果
 */
@Data
@Builder
public class UploadResult {

    /** 原始文件名 */
    private String fileName;

    /** 保存的文件名（UUID生成） */
    private String savedName;

    /** 服务器存储相对路径 */
    private String filePath;

    /** 访问URL */
    private String url;

    /** 文件大小（字节） */
    private long fileSize;

    /** 文件类型（扩展名） */
    private String fileType;
}
