package com.ghf.fcg.modules.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminDailyStatsVO {

    private Integer todayPendingCount;
    private Integer todayDoneCount;
    private Integer todaySkippedCount;
    private Integer completionRate;
    private Integer weeklyVitalCount;
    private List<AdminMemberRateVO> memberRates;
    private List<AdminDailyVitalCountVO> dailyVitalCounts;
}

