package com.ghf.fcg.modules.system.vo;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String avatar;
    private Integer role;
    private Long familyId;
    private Integer careMode;
    private String token;
    private LocalDateTime createTime;
}
