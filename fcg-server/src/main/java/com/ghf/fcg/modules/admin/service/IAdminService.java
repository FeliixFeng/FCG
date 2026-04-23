package com.ghf.fcg.modules.admin.service;

import com.ghf.fcg.common.result.PageResult;
import com.ghf.fcg.modules.admin.vo.AdminDailyStatsVO;
import com.ghf.fcg.modules.admin.vo.AdminOverviewVO;
import com.ghf.fcg.modules.admin.vo.AdminPlanTodayItemVO;

public interface IAdminService {

    AdminOverviewVO getOverview(Long familyId);

    PageResult<AdminPlanTodayItemVO> listTodayPlans(
            Long familyId,
            Long userId,
            Integer status,
            String slotName,
            String keyword,
            long page,
            long size
    );

    AdminDailyStatsVO getDailyStats(Long familyId, Integer days);
}
