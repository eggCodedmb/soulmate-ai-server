package com.soulmate.web.controller;

import com.soulmate.ai.tts.TtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 语音合成 (TTS) 接口 - 接入系统 TTS
 */
@Slf4j
@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TtsController {

    private final TtsService ttsService;

    /**
     * 生成语音 (WAV 格式)
     *
     * 支持 POST /api/tts/generate
     * Body: { "text": "...", "profileId": "..." }
     */
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(@RequestBody Map<String, Object> body) {
        String text = (String) body.get("text");
        String profileId = (String) body.get("profileId");
        if (profileId == null || profileId.isBlank()) {
            profileId = (String) body.get("voiceId");
        }
        if (profileId == null || profileId.isBlank()) {
            profileId = (String) body.get("profile_id");
        }

        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            byte[] audioBytes = ttsService.generateTts(text, profileId);
            if (audioBytes == null || audioBytes.length == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/wav"));
            headers.setContentLength(audioBytes.length);

            return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("系统 TTS 语音合成失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 兼容性流式生成接口
     */
    @PostMapping("/generate/stream")
    public ResponseEntity<byte[]> generateStream(@RequestBody Map<String, Object> body) {
        return generate(body);
    }
}
