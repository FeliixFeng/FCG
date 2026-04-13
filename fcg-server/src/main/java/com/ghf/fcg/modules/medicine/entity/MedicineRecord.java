package com.ghf.fcg.modules.medicine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("med_record")
public class MedicineRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Long userId;

    private Long medicineId;

    private LocalDate scheduledDate;

    private String slotName;

    private LocalDateTime actualTime;

    private Integer status;

    private String recordRemark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_TAKEN = 1;
    public static final int STATUS_SKIPPED = 2;
}