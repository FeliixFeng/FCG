package com.ghf.fcg.modules.medicine.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class MedicineRecordVO {

    private Long id;
    private Long planId;
    private Long userId;
    private Long familyId;
    private Long medicineId;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private LocalDateTime actualTime;
    private Integer status;
    private String notes;
    private LocalDateTime createTime;
}
