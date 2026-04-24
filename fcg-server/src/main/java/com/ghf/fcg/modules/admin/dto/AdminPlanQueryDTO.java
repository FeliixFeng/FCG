package com.ghf.fcg.modules.admin.dto;

import lombok.Data;

@Data
public class AdminPlanQueryDTO {

    private Long userId;
    private Integer status;
    private String slotName;
    private String keyword;
    private Long page = 1L;
    private Long size = 20L;
}
