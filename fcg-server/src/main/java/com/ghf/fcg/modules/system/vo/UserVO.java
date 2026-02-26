package com.ghf.fcg.modules.system.vo;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * 成员信息 VO
 * 选人后返回，token 中携带 familyId + memberId + role
 */
@Data
@Builder
public class UserVO {

    private Long id;
    private String nickname;
    private String phone;
    private String avatar;
    private Integer role;       // 0-管理员 1-普通成员 2-关怀成员
    private Long familyId;
    private String relation;
    private Integer careMode;
    private String token;       // 成员级 token（含 familyId + memberId + role）
    private LocalDateTime createTime;
}
