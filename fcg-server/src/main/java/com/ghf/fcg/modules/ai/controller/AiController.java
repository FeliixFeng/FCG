package com.ghf.fcg.modules.ai.controller;

import com.ghf.fcg.modules.ai.service.AiService;
import com.ghf.fcg.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI智能助手", description = "AI大模型调用接口")
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    @Operation(summary = "AI对话")
    public Result<String> chat(@RequestBody ChatRequest request) {
        String response = aiService.chat(request.getSystemPrompt(), request.getUserPrompt());
        return Result.success(response);
    }

    @PostMapping("/medicine/optimize")
    @Operation(summary = "药品OCR结果AI优化")
    public Result<String> optimizeMedicine(@RequestBody OptimizeMedicineRequest request) {
        String response = aiService.optimizeMedicineInfo(request.getOcrText());
        return Result.success(response);
    }

    @PostMapping("/medicine/advice")
    @Operation(summary = "用药建议")
    public Result<String> getMedicineAdvice(@RequestBody MedicineAdviceRequest request) {
        String response = aiService.generateMedicineAdvice(request.getMedicineName(), request.getUserInfo());
        return Result.success(response);
    }

    @PostMapping("/health/report/summary")
    @Operation(summary = "健康周报AI总结")
    public Result<String> generateHealthReportSummary(@RequestBody HealthReportRequest request) {
        String response = aiService.generateHealthReportSummary(request.getVitalData(), request.getMedicineData());
        return Result.success(response);
    }

    // ========== DTO ==========

    public static class ChatRequest {
        private String systemPrompt;
        private String userPrompt;

        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public String getUserPrompt() { return userPrompt; }
        public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }
    }

    public static class OptimizeMedicineRequest {
        private String ocrText;

        public String getOcrText() { return ocrText; }
        public void setOcrText(String ocrText) { this.ocrText = ocrText; }
    }

    public static class MedicineAdviceRequest {
        private String medicineName;
        private String userInfo;

        public String getMedicineName() { return medicineName; }
        public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
        public String getUserInfo() { return userInfo; }
        public void setUserInfo(String userInfo) { this.userInfo = userInfo; }
    }

    public static class HealthReportRequest {
        private String vitalData;
        private String medicineData;

        public String getVitalData() { return vitalData; }
        public void setVitalData(String vitalData) { this.vitalData = vitalData; }
        public String getMedicineData() { return medicineData; }
        public void setMedicineData(String medicineData) { this.medicineData = medicineData; }
    }
}
