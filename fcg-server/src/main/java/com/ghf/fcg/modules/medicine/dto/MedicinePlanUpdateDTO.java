package com.ghf.fcg.modules.medicine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MedicinePlanUpdateDTO {

    @Schema(description = "单次剂量")
    private BigDecimal dosage;

    @Schema(description = "提醒时段（早,中,晚）")
    private String remindSlots;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "服药星期")
    private String takeDays;

    @Schema(description = "用药注意")
    private String planRemark;

    @Schema(description = "状态 0-停用 1-启用")
    @Min(value = 0) @Max(value = 1)
    private Integer status;
}