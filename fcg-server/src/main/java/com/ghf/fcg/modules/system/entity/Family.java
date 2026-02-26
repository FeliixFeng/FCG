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

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
