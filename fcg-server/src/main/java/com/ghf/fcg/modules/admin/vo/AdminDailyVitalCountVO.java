package com.ghf.fcg.modules.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AdminDailyVitalCountVO {

    private LocalDate day;
    private Integer count;
}

