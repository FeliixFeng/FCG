package com.ghf.fcg.modules.health.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class VitalVO {

    private Long id;
    private Long userId;
    private Long familyId;
    private Integer type;
    private BigDecimal valueSystolic;
    private BigDecimal valueDiastolic;
    private BigDecimal value;
    private String unit;
    private LocalDateTime measureTime;
    private Integer measurePoint;
    private String notes;
    private LocalDateTime createTime;
}
