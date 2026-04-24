package com.ghf.fcg.modules.admin.controller;

import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.context.UserContext;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.common.result.PageResult;
import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.admin.dto.AdminPlanQueryDTO;
import com.ghf.fcg.modules.admin.service.IAdminService;
import com.ghf.fcg.modules.admin.vo.AdminDailyStatsVO;
import com.ghf.fcg.modules.admin.vo.AdminOverviewVO;
import com.ghf.fcg.modules.admin.vo.AdminPlanTodayItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理员模块", description = "管理员跨成员总览与统计接口")
public class AdminController {

    private final IAdminService adminService;

    @GetMapping("/dashboard/overview")
    @Operation(summary = "管理端总览数据")
    public Result<AdminOverviewVO> overview() {
        Long familyId = requireAdminAndFamilyId();
        return Result.success(adminService.getOverview(familyId));
    }

    @GetMapping("/plans/today")
    @Operation(summary = "管理端今日计划总览")
    public Result<PageResult<AdminPlanTodayItemVO>> todayPlans(
            @ParameterObject AdminPlanQueryDTO query) {
        Long familyId = requireAdminAndFamilyId();
        return Result.success(adminService.listTodayPlans(
                familyId,
                query.getUserId(),
                query.getStatus(),
                query.getSlotName(),
                query.getKeyword(),
                query.getPage(),
                query.getSize()
        ));
    }

    @GetMapping("/stats/daily")
    @Operation(summary = "管理端日统计数据")
    public Result<AdminDailyStatsVO> dailyStats(@RequestParam(defaultValue = "7") Integer days) {
        Long familyId = requireAdminAndFamilyId();
        return Result.success(adminService.getDailyStats(familyId, days));
    }

    private Long requireAdminAndFamilyId() {
        UserContext.UserInfo user = UserContext.get();
        if (user == null || user.getRole() == null || user.getRole() != 0) {
            throw new BusinessException(MessageConstant.NO_PERMISSION);
        }
        if (user.getFamilyId() == null) {
            throw new BusinessException(MessageConstant.USER_NOT_IN_FAMILY);
        }
        return user.getFamilyId();
    }
}
