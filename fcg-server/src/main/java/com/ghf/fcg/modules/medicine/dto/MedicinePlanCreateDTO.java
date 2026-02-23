package com.ghf.fcg.modules.medicine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MedicinePlanCreateDTO {

    @Schema(description = "使用者ID")
    @NotNull(message = "使用者不能为空")
    private Long userId;

    @Schema(description = "药品ID")
    @NotNull(message = "药品不能为空")
    private Long medicineId;

    @Schema(description = "单次剂量")
    @NotBlank(message = "剂量不能为空")
    private String dosage;

    @Schema(description = "服药频率")
    @NotBlank(message = "频率不能为空")
    private String frequency;

    @Schema(description = "提醒时间点")
    @NotBlank(message = "提醒时间不能为空")
    private String remindTimes;

    @Schema(description = "开始日期")
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "服药星期")
    private String takeDays;

    @Schema(description = "备注")
    private String notes;

    @Schema(description = "状态 0-停用 1-启用")
    @Min(value = 0, message = "状态范围为0-1")
    @Max(value = 1, message = "状态范围为0-1")
    private Integer status;
}
