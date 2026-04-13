package com.ghf.fcg.modules.medicine.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MedicineRecordVO {

    private Long id;
    private Long planId;
    private Long userId;
    private Long medicineId;
    private LocalDate scheduledDate;
    private String slotName;
    private LocalDateTime actualTime;
    private Integer status;
    private String recordRemark;
    private LocalDateTime createTime;
}