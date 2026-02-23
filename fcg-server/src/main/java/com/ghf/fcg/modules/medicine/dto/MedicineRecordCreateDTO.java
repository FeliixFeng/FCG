package com.ghf.fcg.modules.medicine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class MedicineRecordCreateDTO {

    @Schema(description = "计划ID")
    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @Schema(description = "用户ID")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "药品ID")
    @NotNull(message = "药品ID不能为空")
    private Long medicineId;

    @Schema(description = "应服药日期")
    @NotNull(message = "应服药日期不能为空")
    private LocalDate scheduledDate;

    @Schema(description = "应服药时间")
    @NotNull(message = "应服药时间不能为空")
    private LocalTime scheduledTime;

    @Schema(description = "实际服药时间")
    private LocalDateTime actualTime;

    @Schema(description = "状态 0-未服 1-已服 2-跳过")
    private Integer status;

    @Schema(description = "备注")
    private String notes;
}
