package com.ghf.fcg.modules.medicine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MedicinePlanUpdateDTO {

    @Schema(description = "使用者ID")
    private Long userId;

    @Schema(description = "药品ID")
    private Long medicineId;

    @Schema(description = "单次剂量")
    private String dosage;

    @Schema(description = "服药频率")
    private String frequency;

    @Schema(description = "提醒时间点")
    private String remindTimes;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "服药星期")
    private String takeDays;

    @Schema(description = "备注")
    private String notes;

    @Schema(description = "状态 0-停用 1-启用")
    private Integer status;
}
