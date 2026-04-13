package com.ghf.fcg.modules.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("health_vital")
public class Vital {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long familyId;

    private Integer type;

    private BigDecimal valueSystolic;

    private BigDecimal valueDiastolic;

    private BigDecimal value;

    private String unit;

    private LocalDateTime measureTime;

    private Integer measurePoint;

    private String notes;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static final int TYPE_BLOOD_PRESSURE = 1;
    public static final int TYPE_BLOOD_SUGAR = 2;
    public static final int TYPE_WEIGHT = 3;

    public static final int POINT_FASTING = 1;
    public static final int POINT_AFTER_MEAL = 2;
}
