package com.soulmate.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件上传配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "soulmate.file")
public class FileProperties {

    /** 文件存储根路径 */
    private String baseDir = "D:/Code/soulmate-ai-server/online";

    /** 允许上传的文件类型（扩展名），为空则不限制 */
    private List<String> allowedTypes = List.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt",
            "mp3", "mp4", "wav"
    );

    /** 单个文件最大大小（MB） */
    private int maxSizeMb = 10;

    /** 文件访问URL前缀 */
    private String urlPrefix = "/files";
}
