package com.ghf.fcg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String phone;

    private String avatar;

    private Integer role;

    private Long familyId;

    private String relation;

    private Integer careMode;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static final int ROLE_ADMIN = 0;
    public static final int ROLE_MEMBER = 1;
    public static final int ROLE_CARE_RECEIVER = 2;

    public static final int CARE_MODE_OFF = 0;
    public static final int CARE_MODE_ON = 1;
}
