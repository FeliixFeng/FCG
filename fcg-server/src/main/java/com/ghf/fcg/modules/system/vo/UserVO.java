package com.ghf.fcg.modules.system.vo;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    private Integer role;
    private Long familyId;
    private String relation;
    private Integer careMode;
    private String token;
    private LocalDateTime createTime;
    
    private LocalDate birthday;
    private BigDecimal height;
    private BigDecimal weight;
    private String disease;
    private String allergy;
    private String remark;
}
