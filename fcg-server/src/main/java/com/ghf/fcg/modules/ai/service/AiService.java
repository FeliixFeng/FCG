package com.ghf.fcg.modules.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghf.fcg.config.AiProperties;
import com.ghf.fcg.modules.medicine.vo.MedicineOcrParsedVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiProperties aiProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 文本补全（对话）
     */
    public String chat(String prompt) {
        return chat(prompt, null);
    }

    /**
     * 文本补全（带系统提示）
     */
    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = new HashMap<>();
        // 使用纯文本模型
        requestBody.put("model", aiProperties.getModel());

        List<Map<String, Object>> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
        }

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);

        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + aiProperties.getApiKey());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            String url = aiProperties.getBaseUrl() + "/chat/completions";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            log.warn("AI response is empty: {}", response.getBody());
            return null;
        } catch (Exception e) {
            log.error("AI chat failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 多模态识别：直接识别药品图片并结构化
     * @param imageBase64 图片base64编码（不含data:image前缀）
     * @return 结构化的药品信息JSON字符串
     */
    public String recognizeMedicineImage(String imageBase64) {
        String systemPrompt = """
            你是一个专业的药品信息识别助手。
            请识别图片中的药品说明书或药盒信息，提取准确的药品信息，并以JSON格式返回。
            只返回JSON，不要其他内容。
            """;

        String userPrompt = """
            请识别图片中的药品信息，提取以下字段并返回JSON格式：
            {
                "name": "药品名称",
                "specification": "规格（如500mg）",
                "manufacturer": "生产厂家",
                "dosageForm": "剂型（如片剂、胶囊）",
                "expireDate": "过期日期(YYYY-MM-DD格式，如果看不到则返回null)",
                "usage": "主要用途/适应症",
                "contraindications": "禁忌人群/药物",
                "sideEffects": "常见副作用",
                "dosage": "用法用量"
            }
            
            如果某项无法识别，请返回null。只返回JSON，不要任何额外说明。
            """;

        return chatWithImage(systemPrompt, userPrompt, imageBase64);
    }

    /**
     * 多模态对话（带图片）
     */
    private String chatWithImage(String systemPrompt, String userPrompt, String imageBase64) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiProperties.getModel());

        List<Map<String, Object>> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
        }

        // 用户消息：包含文本和图片
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        
        List<Map<String, Object>> contentList = new ArrayList<>();
        
        // 文本内容
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", userPrompt);
        contentList.add(textContent);
        
        // 图片内容
        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image_url");
        Map<String, String> imageUrl = new HashMap<>();
        imageUrl.put("url", "data:image/jpeg;base64," + imageBase64);
        imageContent.put("image_url", imageUrl);
        contentList.add(imageContent);
        
        userMsg.put("content", contentList);
        messages.add(userMsg);

        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + aiProperties.getApiKey());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            String url = aiProperties.getBaseUrl() + "/chat/completions";
            log.info("Calling AI Vision API: {}, model: {}", url, aiProperties.getModel());
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    log.info("AI Vision response received, length: {}", content != null ? content.length() : 0);
                    return content;
                }
            }
            log.warn("AI Vision response is empty: {}", response.getBody());
            return null;
        } catch (Exception e) {
            log.error("AI Vision call failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI视觉识别服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 优化药品OCR结果
     * 将OCR识别的文字优化为结构化信息
     */
    public String optimizeMedicineInfo(String ocrText) {
        String systemPrompt = """
            你是一个专业的药品信息提取助手。
            你的任务是从OCR识别的文本中提取准确的药品信息，并以JSON格式返回。
            只返回JSON，不要其他内容。
            """;

        String userPrompt = """
            以下是OCR识别的药品信息文本：
            
            %s
            
            请提取以下信息并返回JSON格式：
            {
                "name": "药品名称",
                "specification": "规格",
                "manufacturer": "生产厂家",
                "dosageForm": "剂型",
                "expireDate": "过期日期(YYYY-MM-DD格式，如果看不到则返回null)",
                "usage": "主要用途/适应症",
                "contraindications": "禁忌人群/药物",
                "sideEffects": "常见副作用",
                "dosage": "用法用量"
            }
            
            如果某项无法识别，请返回null。只返回JSON。
            """.formatted(ocrText);

        return chat(systemPrompt, userPrompt);
    }

    public MedicineOcrParsedVO parseMedicineInfo(String aiRawResponse) {
        String jsonPayload = extractJsonPayload(aiRawResponse);
        if (jsonPayload == null) {
            throw new RuntimeException("AI返回格式不正确，未找到JSON对象");
        }

        try {
            Map<String, Object> map = objectMapper.readValue(jsonPayload, Map.class);
            String usage = normalizeString(map.get("usage"));
            String dosage = normalizeString(map.get("dosage"));
            String instructions = normalizeString(map.get("instructions"));

            if (instructions == null) {
                instructions = buildInstructions(usage, dosage);
            }

            String expireDateRaw = normalizeString(map.get("expireDate"));
            if (expireDateRaw == null) {
                expireDateRaw = normalizeString(map.get("expirationDate"));
            }

            return MedicineOcrParsedVO.builder()
                    .name(normalizeString(map.get("name")))
                    .specification(normalizeString(map.get("specification")))
                    .manufacturer(normalizeString(map.get("manufacturer")))
                    .dosageForm(normalizeString(map.get("dosageForm")))
                    .instructions(instructions)
                    .contraindications(normalizeString(map.get("contraindications")))
                    .sideEffects(normalizeString(map.get("sideEffects")))
                    .usage(usage)
                    .dosage(dosage)
                    .expireDate(parseDate(expireDateRaw))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("AI结构化结果解析失败: " + e.getMessage(), e);
        }
    }

    public String currentModel() {
        return aiProperties.getModel();
    }

    private String extractJsonPayload(String aiRawResponse) {
        if (aiRawResponse == null) {
            return null;
        }

        String trimmed = aiRawResponse.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }

        return trimmed.substring(start, end + 1);
    }

    private String normalizeString(Object value) {
        if (value == null) {
            return null;
        }

        String normalized = String.valueOf(value).trim();
        if (normalized.isEmpty() || "null".equalsIgnoreCase(normalized)) {
            return null;
        }

        return normalized;
    }

    private String buildInstructions(String usage, String dosage) {
        if (usage == null && dosage == null) {
            return null;
        }

        if (usage != null && dosage != null) {
            return "适应症：" + usage + "\n用法用量：" + dosage;
        }

        if (usage != null) {
            return "适应症：" + usage;
        }

        return "用法用量：" + dosage;
    }

    private LocalDate parseDate(String rawDate) {
        if (rawDate == null) {
            return null;
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("yyyy.MM.dd"),
                DateTimeFormatter.ofPattern("yyyy年M月d日")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(rawDate, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
    }

    /**
     * 生成用药建议
     */
    public String generateMedicineAdvice(String medicineName, String userInfo) {
        String userPrompt = """
            药品名称：%s
            用户信息：%s
            
            请给出合理的用药建议，包括：
            1. 注意事项
            2. 可能的不良反应
            3. 与其他药物的相互作用提醒
            
            请用通俗易懂的语言回答，适合老年人理解。
            """.formatted(medicineName, userInfo);

        return chat("你是一个专业的用药顾问，请给出安全、合理的建议。", userPrompt);
    }

    /**
     * 生成健康周报AI总结
     */
    public String generateHealthReportSummary(String vitalData, String medicineData) {
        String userPrompt = """
            体征数据：%s
            用药数据：%s
            
            请分析以上数据，生成一份健康周报总结，包括：
            1. 本周健康状况概述
            2. 需要注意的健康风险
            3. 用药依从性评估
            4. 具体的健康建议
            
            请用通俗易懂的语言回答。
            """.formatted(vitalData, medicineData);

        return chat("你是一个专业的健康顾问，请给出科学、合理的健康建议。", userPrompt);
    }
}
