package com.ghf.fcg.modules.medicine.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class MedicinePlanRecordVO {

    private Long recordId;
    private Long planId;
    private Long userId;
    private Long medicineId;
    private String medicineName;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private LocalDateTime actualTime;
    private Integer recordStatus;
    private String recordNotes;

    private String planDosage;
    private String planFrequency;
    private String planRemindTimes;
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private String planTakeDays;
    private String planNotes;
    private Integer planStatus;
}
