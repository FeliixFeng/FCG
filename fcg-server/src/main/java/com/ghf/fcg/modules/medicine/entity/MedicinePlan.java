package com.ghf.fcg.modules.medicine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("med_plan")
public class MedicinePlan {

    @TableId(type = IdType.AUTO)
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

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;
}