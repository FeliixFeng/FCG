package com.ghf.fcg.modules.admin.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminOverviewVO {

    private Integer memberCount;
    private Integer medicineCount;
    private Integer todayPendingCount;
    private Integer todayDoneCount;
    private Integer todaySkippedCount;
    private Integer lowStockCount;
    private Integer expiringSoonCount;
}

