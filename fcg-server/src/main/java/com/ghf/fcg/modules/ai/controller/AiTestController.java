package com.ghf.fcg.modules.ai.controller;

import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.ai.service.AiService;
import com.ghf.fcg.modules.medicine.vo.MedicineOcrParsedVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai/test")
@RequiredArgsConstructor
@Tag(name = "AI测试", description = "测试AI功能")
public class AiTestController {

    private final AiService aiService;

    @PostMapping("/chat")
    @Operation(summary = "测试文本对话")
    public Result<String> testChat(@RequestParam String message) {
        try {
            log.info("Testing AI chat with message: {}", message);
            String response = aiService.chat(message);
            return Result.success(response);
        } catch (Exception e) {
            log.error("AI chat test failed", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/vision")
    @Operation(summary = "测试图片识别")
    public Result<Map<String, Object>> testVision(@RequestPart("file") MultipartFile file) {
        try {
            log.info("Testing AI vision with file: {}", file.getOriginalFilename());
            
            // 转换为base64
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            log.info("Image converted to base64, length: {}", base64.length());
            
            // 调用识别
            List<String> imageList = Collections.singletonList(base64);
            String aiResponse = aiService.recognizeMedicineImage(imageList);
            log.info("AI response: {}", aiResponse);
            
            // 尝试解析
            MedicineOcrParsedVO parsed = null;
            try {
                parsed = aiService.parseMedicineInfo(aiResponse);
            } catch (Exception e) {
                log.warn("Failed to parse AI response: {}", e.getMessage());
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("rawResponse", aiResponse);
            result.put("parsed", parsed);
            result.put("model", aiService.currentModel());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("AI vision test failed", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/config")
    @Operation(summary = "查看AI配置")
    public Result<Map<String, String>> getConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("model", aiService.currentModel());
        config.put("status", "configured");
        return Result.success(config);
    }
}
