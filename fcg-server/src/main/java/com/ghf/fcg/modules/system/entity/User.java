package com.ghf.fcg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 昵称（必填，选人页展示用） */
    private String nickname;

    private String phone;

    /** 头像URL（存OSS地址） */
    private String avatar;

    /** 角色：0-管理员 1-普通成员 2-关怀成员（老人） */
    private Integer role;

    /** 所属家庭ID */
    private Long familyId;

    /** 家庭关系（父亲/母亲/爷爷等） */
    private String relation;

    /** 关怀模式：0-关闭 1-开启 */
    private Integer careMode;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // 角色常量
    public static final int ROLE_ADMIN = 0;          // 管理员（可进入管理员界面）
    public static final int ROLE_MEMBER = 1;          // 普通成员
    public static final int ROLE_CARE = 2;            // 关怀成员（老人，开启关怀界面）

    // 关怀模式常量
    public static final int CARE_MODE_OFF = 0;
    public static final int CARE_MODE_ON = 1;
}
