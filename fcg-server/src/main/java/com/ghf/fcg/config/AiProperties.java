package com.ghf.fcg.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 模型名称，默认 glm-4.6v-flashx（视觉+文本多模态模型）
     */
    private String model = "glm-4.6v-flashx";

    /**
     * 请求基础URL
     */
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";

    /**
     * 超时时间（毫秒）
     */
    private Integer timeout = 30000;
}
