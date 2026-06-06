package com.soulmate.service.impl;

import com.soulmate.common.config.FileProperties;
import com.soulmate.common.exception.BizException;
import com.soulmate.common.response.ResultCode;
import com.soulmate.domain.dto.UploadResult;
import com.soulmate.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileProperties fileProperties;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public UploadResult upload(MultipartFile file) {
        // 校验文件
        validateFile(file);

        // 获取文件信息
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);

        // 生成存储路径：baseDir/yyyy/MM/dd/UUID.ext
        String dateDir = LocalDate.now().format(DATE_FORMAT);
        String savedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String relativePath = dateDir + "/" + savedName;

        Path targetPath = Paths.get(fileProperties.getBaseDir(), relativePath);

        try {
            // 创建目录
            Files.createDirectories(targetPath.getParent());
            // 保存文件
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            log.error("文件上传失败: {}", originalFilename, e);
            throw new BizException(ResultCode.FILE_UPLOAD_FAILED);
        }

        // 构建访问URL
        String url = fileProperties.getUrlPrefix() + "/" + relativePath;

        log.info("文件上传成功: {} -> {}", originalFilename, relativePath);

        return UploadResult.builder()
                .fileName(originalFilename)
                .savedName(savedName)
                .filePath(relativePath)
                .url(url)
                .fileSize(file.getSize())
                .fileType(extension)
                .build();
    }

    @Override
    public List<UploadResult> uploadBatch(List<MultipartFile> files) {
        List<UploadResult> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(upload(file));
        }
        return results;
    }

    @Override
    public void delete(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }

        Path targetPath = Paths.get(fileProperties.getBaseDir(), filePath);

        if (!Files.exists(targetPath)) {
            throw new BizException(ResultCode.FILE_NOT_FOUND);
        }

        try {
            Files.delete(targetPath);
            log.info("文件删除成功: {}", filePath);
        } catch (IOException e) {
            log.error("文件删除失败: {}", filePath, e);
            throw new BizException(ResultCode.FILE_DELETE_FAILED);
        }
    }

    /**
     * 校验文件类型和大小
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "上传文件不能为空");
        }

        // 校验文件大小
        long maxSizeBytes = (long) fileProperties.getMaxSizeMb() * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new BizException(ResultCode.FILE_SIZE_EXCEEDED);
        }

        // 校验文件类型
        String extension = getExtension(file.getOriginalFilename());
        List<String> allowedTypes = fileProperties.getAllowedTypes();
        if (allowedTypes != null && !allowedTypes.isEmpty() && !allowedTypes.contains(extension.toLowerCase())) {
            throw new BizException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
