package com.soulmate.service;

import com.soulmate.domain.dto.UploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传服务接口
 */
public interface FileService {

    /**
     * 上传单个文件
     *
     * @param file 文件
     * @return 上传结果
     */
    UploadResult upload(MultipartFile file);

    /**
     * 批量上传文件
     *
     * @param files 文件列表
     * @return 上传结果列表
     */
    List<UploadResult> uploadBatch(List<MultipartFile> files);

    /**
     * 删除文件
     *
     * @param filePath 文件相对路径
     */
    void delete(String filePath);
}
