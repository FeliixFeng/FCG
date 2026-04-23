package com.ghf.fcg.modules.admin.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminMemberRateVO {

    private Long userId;
    private String userName;
    private Integer doneCount;
    private Integer totalCount;
    private Integer completionRate;
}

