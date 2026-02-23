package com.ghf.fcg.modules.medicine.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MedicinePlanVO {

    private Long id;
    private Long userId;
    private Long familyId;
    private Long medicineId;
    private String dosage;
    private String frequency;
    private String remindTimes;
    private LocalDate startDate;
    private LocalDate endDate;
    private String takeDays;
    private String notes;
    private Integer status;
    private LocalDateTime createTime;
}
