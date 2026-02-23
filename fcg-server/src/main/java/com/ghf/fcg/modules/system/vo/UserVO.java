package com.ghf.fcg.modules.system.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer role;
    private Long familyId;
    private Integer careMode;
    private LocalDateTime createTime;
}
