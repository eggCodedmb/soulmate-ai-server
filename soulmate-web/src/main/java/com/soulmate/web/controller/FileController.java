package com.soulmate.web.controller;

import com.soulmate.common.response.R;
import com.soulmate.domain.dto.UploadResult;
import com.soulmate.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 单文件上传
     *
     * @param file 文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public R<UploadResult> upload(@RequestParam("file") MultipartFile file) {
        UploadResult result = fileService.upload(file);
        return R.ok(result);
    }

    /**
     * 批量文件上传
     *
     * @param files 文件列表
     * @return 上传结果列表
     */
    @PostMapping("/upload/batch")
    public R<List<UploadResult>> uploadBatch(@RequestParam("files") List<MultipartFile> files) {
        List<UploadResult> results = fileService.uploadBatch(files);
        return R.ok(results);
    }

    /**
     * 删除文件
     *
     * @param filePath 文件相对路径
     * @return 操作结果
     */
    @DeleteMapping("/delete")
    public R<Void> delete(@RequestParam("filePath") String filePath) {
        fileService.delete(filePath);
        return R.ok();
    }
}
