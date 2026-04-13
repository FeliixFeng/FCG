package com.ghf.fcg.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String apiKey;

    private String model = "glm-4.6v-flashx";

    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";

    private Integer timeout = 30000;
}
