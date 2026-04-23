package com.ghf.fcg.modules.system.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
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
    private String token;
    private Integer lowStockThreshold;
    private Integer expiringDays;
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
        private Integer role;
        private String relation;
        private String phone;
        private Integer careMode;
        private LocalDateTime joinTime;
        private LocalDate birthday;
        private java.math.BigDecimal height;
        private java.math.BigDecimal weight;
        private String disease;
        private String allergy;
    }
}
