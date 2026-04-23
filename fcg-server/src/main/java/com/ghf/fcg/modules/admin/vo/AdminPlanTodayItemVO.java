package com.ghf.fcg.modules.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminPlanTodayItemVO {

    private Long recordId;
    private Long planId;
    private Long userId;
    private String userName;
    private Long medicineId;
    private String medicineName;
    private String medicineImageUrl;
    private Integer medicineStock;
    private String medicineStockUnit;
    private LocalDate scheduledDate;
    private String slotName;
    private LocalDateTime actualTime;
    private Integer recordStatus;

    private BigDecimal planDosage;
    private String planRemindSlots;
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private String planTakeDays;
    private Integer planStatus;
}

