package com.ghf.fcg.modules.health.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VitalUpdateDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "类型 1-血压 2-血糖 3-心率 4-体温 5-体重")
    private Integer type;

    @Schema(description = "收缩压")
    private BigDecimal valueSystolic;

    @Schema(description = "舒张压")
    private BigDecimal valueDiastolic;

    @Schema(description = "数值")
    private BigDecimal value;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "测量时间")
    private LocalDateTime measureTime;

    @Schema(description = "测量时点 1-空腹 2-餐后")
    private Integer measurePoint;

    @Schema(description = "备注")
    private String notes;
}
