package com.ghf.fcg.modules.health.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @DecimalMin(value = "0", message = "依从性范围为0-100")
    @DecimalMax(value = "100", message = "依从性范围为0-100")
    private BigDecimal complianceRate;

    @Schema(description = "体征摘要(JSON)")
    private String vitalSummary;

    @Schema(description = "AI健康建议")
    private String aiSummary;

    @Schema(description = "风险等级 0-低 1-中 2-高")
    @Min(value = 0, message = "风险等级范围为0-2")
    @Max(value = 2, message = "风险等级范围为0-2")
    private Integer riskLevel;
}
