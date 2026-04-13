package com.ghf.fcg.modules.health.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class HealthReportVO {

    private Long id;
    private Long userId;
    private String userName;
    private Long familyId;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private BigDecimal complianceRate;
    private String vitalSummary;
    private String aiSummary;
    private Integer riskLevel;
    private LocalDateTime createTime;
}
