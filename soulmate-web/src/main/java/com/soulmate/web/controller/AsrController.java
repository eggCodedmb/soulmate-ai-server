package com.soulmate.web.controller;

import com.soulmate.ai.asr.AsrService;
import com.soulmate.common.config.AiProperties;
import com.soulmate.common.response.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * 语音识别接口
 */
@Slf4j
@RestController
@RequestMapping("/api/asr")
@RequiredArgsConstructor
public class AsrController {

    private final AsrService asrService;
    private final AiProperties aiProperties;

    /** 支持的音频格式（小米 ASR 仅支持 wav 和 mp3） */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "audio/wav", "audio/wave", "audio/x-wav",
            "audio/mpeg", "audio/mp3"
    );

    /**
     * 语音转文字
     *
     * @param audio 音频文件（支持 WAV, MP3, M4A, WEBM, OGG, FLAC）
     * @return 识别出的文字
     */
    @PostMapping("/transcribe")
    public R<String> transcribe(@RequestParam("audio") MultipartFile audio) {
        // 1. 校验文件是否为空
        if (audio.isEmpty()) {
            return R.fail("音频文件不能为空");
        }

        // 2. 校验文件格式
        String contentType = audio.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            return R.fail("不支持的音频格式，请上传 WAV 或 MP3 格式");
        }

        // 3. 校验文件大小
        int maxSizeMb = aiProperties.getAsr().getMaxSizeMb();
        long maxSizeBytes = (long) maxSizeMb * 1024 * 1024;
        if (audio.getSize() > maxSizeBytes) {
            return R.fail("音频文件不能超过" + maxSizeMb + "MB");
        }

        // 4. 调用 ASR 服务
        try {
            String text = asrService.transcribe(audio.getBytes(), audio.getOriginalFilename());
            if (text == null || text.isBlank()) {
                return R.fail("语音识别失败，未识别出文字");
            }
            return R.ok(text);
        } catch (Exception e) {
            log.error("语音转文字异常", e);
            return R.fail("语音识别服务异常，请稍后再试");
        }
    }
}
