package com.ghf.fcg.modules.medicine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("med_medicine")
public class Medicine {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long familyId;

    private String name;

    private String specification;

    private String imageUrl;

    private Integer stock;

    private String stockUnit;

    private LocalDate expireDate;

    private String usageNotes;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
