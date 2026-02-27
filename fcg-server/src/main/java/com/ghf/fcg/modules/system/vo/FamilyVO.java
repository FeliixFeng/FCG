package com.ghf.fcg.modules.system.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭信息 VO
 */
@Data
@Builder
public class FamilyVO {

    private Long id;
    private String familyName;
    private String username;
    /** 家庭级 token（登录后返回，用于获取成员列表） */
    private String token;
    private LocalDateTime createTime;

    /**
     * 家庭成员信息（选人页展示用）
     */
    @Data
    @Builder
    public static class MemberInfo {
        private Long userId;
        private String nickname;
        private String avatar;
        private Integer role;       // 0-管理员 1-普通成员 2-关怀成员
        private String relation;
        private Integer careMode;
        private LocalDateTime joinTime;
    }
}
