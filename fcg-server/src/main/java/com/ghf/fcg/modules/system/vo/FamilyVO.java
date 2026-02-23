package com.ghf.fcg.modules.system.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FamilyVO {

    private Long id;
    private String familyName;
    private String inviteCode;
    private LocalDateTime createTime;

    @Data
    @Builder
    public static class MemberInfo {
        private Long userId;
        private String username;
        private String nickname;
        private Integer role;
        private String relation;
        private Integer careMode;
        private LocalDateTime joinTime;
    }
}
