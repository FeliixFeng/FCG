package com.ghf.fcg.modules.medicine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MedicineRecordUpdateDTO {

    @Schema(description = "应服日期")
    private LocalDate scheduledDate;

    @Schema(description = "应服时段（早/中/晚/睡前）")
    private String slotName;

    @Schema(description = "实际打卡时间")
    private LocalDateTime actualTime;

    @Schema(description = "状态 0-未服 1-已服 2-跳过")
    @Min(0) @Max(2)
    private Integer status;

    @Schema(description = "记录备注")
    private String recordRemark;
}