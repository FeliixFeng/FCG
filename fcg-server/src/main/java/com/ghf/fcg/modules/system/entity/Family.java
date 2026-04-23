package com.ghf.fcg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_family")
public class Family {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 家庭名称 */
    private String familyName;

    /** 家庭账号（登录用，全局唯一） */
    private String username;

    /** 家庭密码（BCrypt加密） */
    private String password;

    /** 创建者成员ID */
    private Long creatorId;

    /** 低库存阈值（库存小于该值视为紧张） */
    private Integer lowStockThreshold;

    /** 临期提醒阈值（距离过期天数 <= 该值视为临期） */
    private Integer expiringDays;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
