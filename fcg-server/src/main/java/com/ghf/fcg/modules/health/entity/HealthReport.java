package com.ghf.fcg.modules.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_report")
public class HealthReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long familyId;

    private LocalDate weekStart;

    private LocalDate weekEnd;

    private BigDecimal complianceRate;

    private String vitalSummary;

    private String aiSummary;

    private Integer riskLevel;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static final int RISK_LOW = 0;
    public static final int RISK_MEDIUM = 1;
    public static final int RISK_HIGH = 2;
}
