package com.ghf.fcg.modules.health.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.dto.PageQuery;
import com.ghf.fcg.common.result.PageResult;
import com.ghf.fcg.common.context.UserContext;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.ai.service.AiService;
import com.ghf.fcg.modules.health.dto.HealthReportCreateDTO;
import com.ghf.fcg.modules.health.dto.HealthReportUpdateDTO;
import com.ghf.fcg.modules.health.entity.HealthReport;
import com.ghf.fcg.modules.health.entity.Vital;
import com.ghf.fcg.modules.health.service.IHealthReportService;
import com.ghf.fcg.modules.health.service.IVitalService;
import com.ghf.fcg.modules.health.vo.HealthReportVO;
import com.ghf.fcg.modules.medicine.service.IMedicineRecordService;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/health/report")
@RequiredArgsConstructor
@Tag(name = "健康周报模块", description = "健康周报管理")
public class HealthReportController {

    private final IHealthReportService reportService;
    private final IUserService userService;
    private final IVitalService vitalService;
    private final IMedicineRecordService medicineRecordService;
    private final AiService aiService;

    @PostMapping("/generate")
    @Operation(summary = "生成健康周报", description = "自动聚合本周数据并调用AI生成健康周报（本周只保留一条）")
    public Result<HealthReportVO> generate(@RequestParam(required = false) Long userId) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);

        Long targetUserId = userId != null ? userId : currentUserId;
        validateFamilyUser(familyId, targetUserId);

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        LocalDateTime startTime = weekStart.atStartOfDay();
        LocalDateTime endTime = weekEnd.atTime(LocalTime.MAX);

        List<Vital> vitals = vitalService.listByUserIdAndDateRange(targetUserId, startTime, endTime);
        String vitalData = formatVitalData(vitals);

        BigDecimal complianceRate = medicineRecordService.calculateComplianceRate(targetUserId, weekStart, weekEnd);

        String aiSummary = aiService.generateHealthReportSummary(vitalData, "本周用药依从率：" + complianceRate + "%");

        int riskLevel = analyzeRiskLevel(vitals, complianceRate);

        HealthReport existing = reportService.getOne(new LambdaQueryWrapper<HealthReport>()
                .eq(HealthReport::getUserId, targetUserId)
                .eq(HealthReport::getWeekStart, weekStart));

        HealthReport report;
        if (existing != null) {
            existing.setComplianceRate(complianceRate);
            existing.setVitalSummary(vitalData);
            existing.setAiSummary(aiSummary);
            existing.setRiskLevel(riskLevel);
            reportService.updateById(existing);
            report = existing;
        } else {
            report = new HealthReport();
            report.setUserId(targetUserId);
            report.setFamilyId(familyId);
            report.setWeekStart(weekStart);
            report.setWeekEnd(weekEnd);
            report.setComplianceRate(complianceRate);
            report.setVitalSummary(vitalData);
            report.setAiSummary(aiSummary);
            report.setRiskLevel(riskLevel);
            reportService.save(report);
        }

        return Result.success(toReportVO(report));
    }

    @GetMapping("/latest")
    @Operation(summary = "获取当前用户最新周报")
    public Result<HealthReportVO> getLatest(@RequestParam(required = false) Long userId) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);

        Long targetUserId = userId != null ? userId : currentUserId;
        validateFamilyUser(familyId, targetUserId);

        HealthReport report = reportService.getOne(new LambdaQueryWrapper<HealthReport>()
                .eq(HealthReport::getUserId, targetUserId)
                .eq(HealthReport::getFamilyId, familyId)
                .orderByDesc(HealthReport::getWeekStart)
                .last("LIMIT 1"));

        if (report == null) {
            return Result.success(null);
        }

        return Result.success(toReportVO(report));
    }

    @PostMapping
    @Operation(summary = "新增健康周报")
    public Result<Long> create(@RequestBody @Valid HealthReportCreateDTO dto) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);
        validateFamilyUser(familyId, dto.getUserId());

        HealthReport report = new HealthReport();
        report.setUserId(dto.getUserId());
        report.setFamilyId(familyId);
        report.setWeekStart(dto.getWeekStart());
        report.setWeekEnd(dto.getWeekEnd());
        report.setComplianceRate(dto.getComplianceRate());
        report.setVitalSummary(dto.getVitalSummary());
        report.setAiSummary(dto.getAiSummary());
        report.setRiskLevel(dto.getRiskLevel());

        reportService.save(report);
        return Result.success(report.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新健康周报")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid HealthReportUpdateDTO dto) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);

        HealthReport report = getFamilyReport(id, familyId);
        applyReportUpdate(report, familyId, dto);
        reportService.updateById(report);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除健康周报")
    public Result<Void> delete(@PathVariable Long id) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);

        HealthReport report = getFamilyReport(id, familyId);
        reportService.removeById(report.getId());
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "健康周报详情")
    public Result<HealthReportVO> get(@PathVariable Long id) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);

        HealthReport report = getFamilyReport(id, familyId);
        return Result.success(toReportVO(report));
    }

    @GetMapping("/list")
    @Operation(summary = "健康周报列表")
    public Result<PageResult<HealthReportVO>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd,
            @RequestParam(required = false) Integer riskLevel,
            @ParameterObject PageQuery query) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);

        LambdaQueryWrapper<HealthReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthReport::getFamilyId, familyId);
        if (userId != null) {
            wrapper.eq(HealthReport::getUserId, userId);
        }
        if (weekStart != null) {
            wrapper.ge(HealthReport::getWeekStart, weekStart);
        }
        if (weekEnd != null) {
            wrapper.le(HealthReport::getWeekEnd, weekEnd);
        }
        if (riskLevel != null) {
            wrapper.eq(HealthReport::getRiskLevel, riskLevel);
        }
        wrapper.orderByDesc(HealthReport::getWeekStart);

        Page<HealthReport> pageResult = reportService.page(new Page<>(query.getPage(), query.getSize()), wrapper);
        return Result.success(PageResult.of(pageResult,
                pageResult.getRecords().stream().map(this::toReportVO).collect(Collectors.toList())));
    }

    private Long requireFamilyId(Long userId) {
        User user = userService.getById(userId);
        if (user == null || user.getFamilyId() == null) {
            throw new BusinessException(MessageConstant.USER_NOT_IN_FAMILY);
        }
        return user.getFamilyId();
    }

    private void validateFamilyUser(Long familyId, Long userId) {
        User user = userService.getById(userId);
        if (user == null || !familyId.equals(user.getFamilyId())) {
            throw new BusinessException(MessageConstant.USER_FAMILY_MISMATCH);
        }
    }

    private HealthReport getFamilyReport(Long id, Long familyId) {
        HealthReport report = reportService.getById(id);
        if (report == null || !familyId.equals(report.getFamilyId())) {
            throw new BusinessException(MessageConstant.REPORT_NOT_EXIST);
        }
        return report;
    }

    private void applyReportUpdate(HealthReport report, Long familyId, HealthReportUpdateDTO dto) {
        if (dto.getUserId() != null) {
            validateFamilyUser(familyId, dto.getUserId());
            report.setUserId(dto.getUserId());
        }
        if (dto.getWeekStart() != null) {
            report.setWeekStart(dto.getWeekStart());
        }
        if (dto.getWeekEnd() != null) {
            report.setWeekEnd(dto.getWeekEnd());
        }
        if (dto.getComplianceRate() != null) {
            report.setComplianceRate(dto.getComplianceRate());
        }
        if (dto.getVitalSummary() != null) {
            report.setVitalSummary(dto.getVitalSummary());
        }
        if (dto.getAiSummary() != null) {
            report.setAiSummary(dto.getAiSummary());
        }
        if (dto.getRiskLevel() != null) {
            report.setRiskLevel(dto.getRiskLevel());
        }
    }

    private HealthReportVO toReportVO(HealthReport report) {
        User user = userService.getById(report.getUserId());
        String userName = user != null ? user.getNickname() : null;
        
        return HealthReportVO.builder()
                .id(report.getId())
                .userId(report.getUserId())
                .userName(userName)
                .familyId(report.getFamilyId())
                .weekStart(report.getWeekStart())
                .weekEnd(report.getWeekEnd())
                .complianceRate(report.getComplianceRate())
                .vitalSummary(report.getVitalSummary())
                .aiSummary(report.getAiSummary())
                .riskLevel(report.getRiskLevel())
                .createTime(report.getCreateTime())
                .build();
    }

    private String formatVitalData(List<Vital> vitals) {
        if (vitals == null || vitals.isEmpty()) {
            return "本周暂无体征数据";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("本周共记录").append(vitals.size()).append("条体征数据：\n");

        long bpCount = vitals.stream().filter(v -> v.getType() == Vital.TYPE_BLOOD_PRESSURE).count();
        long bsCount = vitals.stream().filter(v -> v.getType() == Vital.TYPE_BLOOD_SUGAR).count();
        long wtCount = vitals.stream().filter(v -> v.getType() == Vital.TYPE_WEIGHT).count();

        if (bpCount > 0) {
            sb.append("- 血压：").append(bpCount).append("次\n");
        }
        if (bsCount > 0) {
            sb.append("- 血糖：").append(bsCount).append("次\n");
        }
        if (wtCount > 0) {
            sb.append("- 体重：").append(wtCount).append("次\n");
        }

        return sb.toString();
    }

    private int analyzeRiskLevel(List<Vital> vitals, BigDecimal complianceRate) {
        if (complianceRate == null) {
            return HealthReport.RISK_LOW;
        }

        double rate = complianceRate.doubleValue();

        if (rate < 60) {
            return HealthReport.RISK_HIGH;
        } else if (rate < 80) {
            return HealthReport.RISK_MEDIUM;
        }

        return HealthReport.RISK_LOW;
    }
}
