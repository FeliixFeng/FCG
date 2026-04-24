package com.ghf.fcg.modules.medicine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghf.fcg.common.dto.PageQuery;
import com.ghf.fcg.common.result.PageResult;
import com.ghf.fcg.modules.medicine.entity.Medicine;
import com.ghf.fcg.modules.medicine.entity.MedicinePlan;
import com.ghf.fcg.modules.medicine.entity.MedicineRecord;
import com.ghf.fcg.modules.medicine.service.IMedicinePlanService;
import com.ghf.fcg.modules.medicine.service.IMedicinePlanTaskService;
import com.ghf.fcg.modules.medicine.service.IMedicineRecordService;
import com.ghf.fcg.modules.medicine.service.IMedicineService;
import com.ghf.fcg.modules.medicine.vo.MedicinePlanRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicinePlanTaskServiceImpl implements IMedicinePlanTaskService {

    private final IMedicinePlanService planService;
    private final IMedicineRecordService recordService;
    private final IMedicineService medicineService;

    @Override
    public PageResult<MedicinePlanRecordVO> buildDailyTasks(Long targetUserId, LocalDate scheduledDate, Integer status, PageQuery query) {
        int day = scheduledDate.getDayOfWeek().getValue();
        String dayToken = String.valueOf(day);

        LambdaQueryWrapper<MedicinePlan> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(MedicinePlan::getUserId, targetUserId)
                .eq(MedicinePlan::getStatus, MedicinePlan.STATUS_ENABLED)
                .le(MedicinePlan::getStartDate, scheduledDate)
                .and(w -> w.isNull(MedicinePlan::getEndDate).or().ge(MedicinePlan::getEndDate, scheduledDate));
        List<MedicinePlan> plans = planService.list(planWrapper);
        if (plans.isEmpty()) {
            Page<MedicinePlanRecordVO> emptyPage = new Page<>(query.getPage(), query.getSize(), 0);
            return PageResult.of(emptyPage, List.of());
        }

        List<MedicinePlan> validPlans = plans.stream()
                .filter(plan -> containsTakeDay(plan.getTakeDays(), dayToken))
                .toList();
        if (validPlans.isEmpty()) {
            Page<MedicinePlanRecordVO> emptyPage = new Page<>(query.getPage(), query.getSize(), 0);
            return PageResult.of(emptyPage, List.of());
        }

        Set<Long> medicineIds = validPlans.stream().map(MedicinePlan::getMedicineId).collect(Collectors.toSet());
        Map<Long, Medicine> medicineMap = medicineService.listByIds(medicineIds).stream()
                .collect(Collectors.toMap(Medicine::getId, item -> item));

        LambdaQueryWrapper<MedicineRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(MedicineRecord::getUserId, targetUserId)
                .eq(MedicineRecord::getScheduledDate, scheduledDate);
        List<MedicineRecord> records = recordService.list(recordWrapper);
        Map<String, MedicineRecord> recordMap = records.stream()
                .collect(Collectors.toMap(
                        r -> buildRecordKey(r.getPlanId(), r.getSlotName()),
                        r -> r,
                        (a, b) -> a.getId() > b.getId() ? a : b
                ));

        List<MedicinePlanRecordVO> allTasks = new ArrayList<>();
        for (MedicinePlan plan : validPlans) {
            List<String> slots = parseSlots(plan.getRemindSlots());
            for (String slot : slots) {
                MedicineRecord record = recordMap.get(buildRecordKey(plan.getId(), slot));
                Integer recordStatus = record != null ? record.getStatus() : MedicineRecord.STATUS_PENDING;
                if (status != null && !status.equals(recordStatus)) {
                    continue;
                }

                Medicine medicine = medicineMap.get(plan.getMedicineId());
                allTasks.add(MedicinePlanRecordVO.builder()
                        .recordId(record != null ? record.getId() : null)
                        .planId(plan.getId())
                        .userId(plan.getUserId())
                        .medicineId(plan.getMedicineId())
                        .medicineName(medicine == null ? null : medicine.getName())
                        .medicineImageUrl(medicine == null ? null : medicine.getImageUrl())
                        .medicineStock(medicine == null ? null : medicine.getStock())
                        .medicineStockUnit(medicine == null ? null : medicine.getStockUnit())
                        .scheduledDate(scheduledDate)
                        .slotName(slot)
                        .actualTime(record != null ? record.getActualTime() : null)
                        .recordStatus(recordStatus)
                        .planDosage(plan.getDosage())
                        .planRemindSlots(plan.getRemindSlots())
                        .planStartDate(plan.getStartDate())
                        .planEndDate(plan.getEndDate())
                        .planTakeDays(plan.getTakeDays())
                        .planRemark(plan.getPlanRemark())
                        .planStatus(plan.getStatus())
                        .build());
            }
        }

        allTasks.sort(Comparator
                .comparingInt((MedicinePlanRecordVO vo) -> slotWeight(vo.getSlotName()))
                .thenComparing(MedicinePlanRecordVO::getPlanId));

        long total = allTasks.size();
        long page = query.getPage();
        long size = query.getSize();
        int fromIndex = (int) Math.max(0, (page - 1) * size);
        int toIndex = (int) Math.min(total, fromIndex + size);
        List<MedicinePlanRecordVO> paged = fromIndex >= toIndex ? List.of() : allTasks.subList(fromIndex, toIndex);

        Page<MedicinePlanRecordVO> pageResult = new Page<>(page, size, total);
        return PageResult.of(pageResult, paged);
    }

    private boolean containsTakeDay(String takeDays, String dayToken) {
        if (takeDays == null || takeDays.isBlank()) {
            return true;
        }
        return Arrays.stream(takeDays.split(","))
                .map(String::trim)
                .anyMatch(dayToken::equals);
    }

    private List<String> parseSlots(String remindSlots) {
        if (remindSlots == null || remindSlots.isBlank()) {
            return List.of("早");
        }
        return Arrays.stream(remindSlots.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private String buildRecordKey(Long planId, String slotName) {
        return planId + "#" + (slotName == null ? "" : slotName.trim());
    }

    private int slotWeight(String slotName) {
        if (slotName == null) return 99;
        return switch (slotName) {
            case "早" -> 1;
            case "中" -> 2;
            case "晚" -> 3;
            case "睡前" -> 4;
            default -> 99;
        };
    }
}
