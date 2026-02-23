package com.ghf.fcg.modules.health.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VitalCreateDTO {

    @Schema(description = "用户ID")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "类型 1-血压 2-血糖 3-心率 4-体温 5-体重")
    @NotNull(message = "体征类型不能为空")
    @Min(value = 1, message = "体征类型范围为1-5")
    @Max(value = 5, message = "体征类型范围为1-5")
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
    @NotNull(message = "测量时间不能为空")
    private LocalDateTime measureTime;

    @Schema(description = "测量时点 1-空腹 2-餐后")
    @Min(value = 1, message = "测量时点范围为1-2")
    @Max(value = 2, message = "测量时点范围为1-2")
    private Integer measurePoint;

    @Schema(description = "备注")
    private String notes;
}
