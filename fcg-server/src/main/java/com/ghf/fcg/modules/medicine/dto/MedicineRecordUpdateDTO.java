package com.ghf.fcg.modules.medicine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class MedicineRecordUpdateDTO {

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "药品ID")
    private Long medicineId;

    @Schema(description = "应服药日期")
    private LocalDate scheduledDate;

    @Schema(description = "应服药时间")
    private LocalTime scheduledTime;

    @Schema(description = "实际服药时间")
    private LocalDateTime actualTime;

    @Schema(description = "状态 0-未服 1-已服 2-跳过")
    private Integer status;

    @Schema(description = "备注")
    private String notes;
}
