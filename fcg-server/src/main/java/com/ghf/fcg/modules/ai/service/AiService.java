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
public String recognizeMedicineImage(List<String> imageBase64List) {
        String systemPrompt = """
            你是一个专业的药品信息识别助手。
            请仔细识别所有图片中的药品信息，并综合所有图片的结果。
            """;

        String userPrompt = """
            请仔细识别图片中的药品信息，从图片中找出以下信息（可能用不同文字描述）：
            
            1. 药品名称：可能叫"药名"、"药品名称"、"商品名称"、"通用名"等
            2. 规格：可能叫"规格"、"剂型"、"含量"、"包装"等
            3. 用法用量：可能叫"用法"、"用量"、"用法用量"、"服用方法"、"使用说明"、"用法说明"、"禁忌"、"注意事项"、"不良反应"等，描述怎么吃、吃多少，有什么禁忌或注意事项
            4. 适应症：可能叫"适应症"、"适应证"、"主治"、"功效"、"用于治疗"、"适用症"等，描述用于治疗什么病
            
            重要：如果图片中找不到适应症，但能识别到药品名称，请根据药品名称推断一个最常见的适应症。
            如果药品名称也识别不到，适应症返回空字符串""。
            
            综合所有图片的结果，取最完整的信息。
            
            返回JSON：
            {"name":"名称","specification":"规格","usageNotes":"用法用量","indication":"适应症"}
            
            只返回JSON，不要其他文字。
            """;

        return chatWithImageList(systemPrompt, userPrompt, imageBase64List);
    }

    public MedicineOcrParsedVO parseMedicineInfo(String aiRawResponse) {
        String jsonPayload = extractJsonPayload(aiRawResponse);
        if (jsonPayload == null) {
            throw new RuntimeException("AI返回格式不正确，未找到JSON对象");
        }

        try {
            Map<String, Object> map = objectMapper.readValue(jsonPayload, Map.class);

            return MedicineOcrParsedVO.builder()
                    .name(normalizeString(map.get("name")))
                    .specification(normalizeString(map.get("specification")))
                    .usageNotes(normalizeString(map.get("usageNotes")))
                    .indication(normalizeString(map.get("indication")))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("AI结构化结果解析失败: " + e.getMessage(), e);
        }
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
        if (normalized.isEmpty() || "null".equalsIgnoreCase(normalized) || "空".equals(normalized)) {
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

    public String currentModel() {
        return aiProperties.getModel();
    }

    public String generateHealthReportSummary(String vitalData, String medicineData) {
        String userPrompt = """
            你将基于“结构化健康数据”生成家庭健康周报。
            请严格基于下面数据，不要编造不存在的指标：

            体征数据：
            %s

            用药数据：
            %s

            请按以下 Markdown 模板输出（每节 2-4 条要点，语言简洁）：
            ## 本周概况
            ## 需要关注的异常信号
            ## 用药依从性解读
            ## 下周建议（可执行）

            要求：
            1. 优先引用“最新值、均值、异常次数”来支撑结论
            2. 建议要具体，避免空泛表述
            3. 如数据不足，请明确写出“数据不足项”
            """.formatted(vitalData, medicineData);

        return chat("你是专业家庭健康管理顾问，擅长把结构化指标转成清晰可执行的周报。", userPrompt);
    }

    private String chatWithImageList(String systemPrompt, String userPrompt, List<String> imageBase64List) {
        Map<String, Object> requestBody = new HashMap<>();
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

        List<Map<String, Object>> contentList = new ArrayList<>();

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", userPrompt);
        contentList.add(textContent);

        for (String imageBase64 : imageBase64List) {
            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image_url");
            Map<String, String> imageUrl = new HashMap<>();
            imageUrl.put("url", "data:image/jpeg;base64," + imageBase64);
            imageContent.put("image_url", imageUrl);
            contentList.add(imageContent);
        }

        userMsg.put("content", contentList);
        messages.add(userMsg);
        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + aiProperties.getApiKey());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            String url = aiProperties.getBaseUrl() + "/chat/completions";
            log.info("Calling AI Vision API with {} images", imageBase64List.size());

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("AI Vision API 调用失败", e);
            throw new RuntimeException("AI图像识别失败: " + e.getMessage(), e);
        }
    }
}
