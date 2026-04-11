package com.ghf.fcg.modules.medicine.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MedicinePlanVO {

    private Long id;
    private Long userId;
    private Long medicineId;
    private BigDecimal dosage;
    private String remindSlots;
    private LocalDate startDate;
    private LocalDate endDate;
    private String takeDays;
    private String planRemark;
    private Integer status;
    private LocalDateTime createTime;
}