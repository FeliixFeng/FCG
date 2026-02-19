package com.ghf.fcg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_family")
public class Family {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String familyName;

    private String inviteCode;

    private Long creatorId;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
