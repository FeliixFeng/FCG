package com.ghf.fcg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sys_user_profile")
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 成员ID（关联 sys_user） */
    private Long userId;

    /** 出生日期 */
    private LocalDate birthday;

    /** 身高（cm） */
    private java.math.BigDecimal height;

    /** 体重（kg） */
    private java.math.BigDecimal weight;

    /** 病史（高血压/糖尿病等，逗号分隔） */
    private String disease;

    /** 过敏史 */
    private String allergy;

    /** 备注 */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}