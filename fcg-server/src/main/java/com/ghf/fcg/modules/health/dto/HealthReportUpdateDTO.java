package com.ghf.fcg.modules.health.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class HealthReportUpdateDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "周开始日期")
    private LocalDate weekStart;

    @Schema(description = "周结束日期")
    private LocalDate weekEnd;

    @Schema(description = "服药依从性")
    private BigDecimal complianceRate;

    @Schema(description = "体征摘要(JSON)")
    private String vitalSummary;

    @Schema(description = "AI健康建议")
    private String aiSummary;

    @Schema(description = "风险等级 0-低 1-中 2-高")
    private Integer riskLevel;
}
