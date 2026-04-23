package com.ghf.fcg.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghf.fcg.common.result.PageResult;
import com.ghf.fcg.modules.admin.service.IAdminService;
import com.ghf.fcg.modules.admin.vo.AdminDailyStatsVO;
import com.ghf.fcg.modules.admin.vo.AdminDailyVitalCountVO;
import com.ghf.fcg.modules.admin.vo.AdminMemberRateVO;
import com.ghf.fcg.modules.admin.vo.AdminOverviewVO;
import com.ghf.fcg.modules.admin.vo.AdminPlanTodayItemVO;
import com.ghf.fcg.modules.health.entity.Vital;
import com.ghf.fcg.modules.health.service.IVitalService;
import com.ghf.fcg.modules.medicine.entity.Medicine;
import com.ghf.fcg.modules.medicine.entity.MedicinePlan;
import com.ghf.fcg.modules.medicine.entity.MedicineRecord;
import com.ghf.fcg.modules.medicine.service.IMedicinePlanService;
import com.ghf.fcg.modules.medicine.service.IMedicineRecordService;
import com.ghf.fcg.modules.medicine.service.IMedicineService;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements IAdminService {

    private final IUserService userService;
    private final IMedicineService medicineService;
    private final IMedicinePlanService planService;
    private final IMedicineRecordService recordService;
    private final IVitalService vitalService;

    @Override
    public AdminOverviewVO getOverview(Long familyId) {
        List<User> members = listFamilyMembers(familyId);
        List<Medicine> medicines = listFamilyMedicines(familyId);
        List<AdminPlanTodayItemVO> todayPlans = buildTodayPlanItems(familyId, null, null);

        int lowStockCount = (int) medicines.stream()
                .filter(med -> med.getStock() != null && med.getStock() < 5)
                .count();

        LocalDate today = LocalDate.now();
        LocalDate expiringDeadline = today.plusDays(30);
        int expiringCount = (int) medicines.stream()
                .filter(med -> med.getExpireDate() != null
                        && !med.getExpireDate().isBefore(today)
                        && !med.getExpireDate().isAfter(expiringDeadline))
                .count();

        int pending = (int) todayPlans.stream().filter(item -> item.getRecordStatus() != null && item.getRecordStatus() == 0).count();
        int done = (int) todayPlans.stream().filter(item -> item.getRecordStatus() != null && item.getRecordStatus() == 1).count();
        int skipped = (int) todayPlans.stream().filter(item -> item.getRecordStatus() != null && item.getRecordStatus() == 2).count();

        return AdminOverviewVO.builder()
                .memberCount(members.size())
                .medicineCount(medicines.size())
                .todayPendingCount(pending)
                .todayDoneCount(done)
                .todaySkippedCount(skipped)
                .lowStockCount(lowStockCount)
                .expiringSoonCount(expiringCount)
                .build();
    }

    @Override
    public PageResult<AdminPlanTodayItemVO> listTodayPlans(Long familyId, Long userId, Integer status, long page, long size) {
        List<AdminPlanTodayItemVO> all = buildTodayPlanItems(familyId, userId, status);
        long safePage = Math.max(page, 1);
        long safeSize = Math.max(size, 1);
        long fromIndex = (safePage - 1) * safeSize;
        if (fromIndex >= all.size()) {
            Page<AdminPlanTodayItemVO> emptyPage = new Page<>(safePage, safeSize, all.size());
            return PageResult.of(emptyPage, List.of());
        }

        long toIndex = Math.min(fromIndex + safeSize, all.size());
        List<AdminPlanTodayItemVO> paged = all.subList((int) fromIndex, (int) toIndex);
        Page<AdminPlanTodayItemVO> resultPage = new Page<>(safePage, safeSize, all.size());
        return PageResult.of(resultPage, paged);
    }

    @Override
    public AdminDailyStatsVO getDailyStats(Long familyId, Integer days) {
        int safeDays = days == null ? 7 : Math.max(1, Math.min(days, 30));
        List<User> members = listFamilyMembers(familyId);
        List<AdminPlanTodayItemVO> todayPlans = buildTodayPlanItems(familyId, null, null);

        int pending = (int) todayPlans.stream().filter(item -> item.getRecordStatus() != null && item.getRecordStatus() == 0).count();
        int done = (int) todayPlans.stream().filter(item -> item.getRecordStatus() != null && item.getRecordStatus() == 1).count();
        int skipped = (int) todayPlans.stream().filter(item -> item.getRecordStatus() != null && item.getRecordStatus() == 2).count();
        int total = pending + done + skipped;
        int completionRate = total == 0 ? 0 : Math.round(done * 100f / total);

        Map<Long, List<AdminPlanTodayItemVO>> groupedByUser = todayPlans.stream()
                .collect(Collectors.groupingBy(AdminPlanTodayItemVO::getUserId));

        List<AdminMemberRateVO> memberRates = new ArrayList<>();
        for (User member : members) {
            List<AdminPlanTodayItemVO> userPlans = groupedByUser.getOrDefault(member.getId(), List.of());
            int userDone = (int) userPlans.stream().filter(item -> item.getRecordStatus() != null && item.getRecordStatus() == 1).count();
            int userTotal = userPlans.size();
            int userRate = userTotal == 0 ? 0 : Math.round(userDone * 100f / userTotal);
            memberRates.add(AdminMemberRateVO.builder()
                    .userId(member.getId())
                    .userName(member.getNickname())
                    .doneCount(userDone)
                    .totalCount(userTotal)
                    .completionRate(userRate)
                    .build());
        }
        memberRates.sort(Comparator.comparingInt(AdminMemberRateVO::getCompletionRate).reversed()
                .thenComparingInt(AdminMemberRateVO::getDoneCount).reversed());

        LocalDate today = LocalDate.now();
        LocalDate startDay = today.minusDays(safeDays - 1L);
        LocalDateTime startTime = startDay.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1L).atStartOfDay().minusNanos(1);

        List<Vital> weeklyVitals = vitalService.list(new LambdaQueryWrapper<Vital>()
                .eq(Vital::getFamilyId, familyId)
                .ge(Vital::getMeasureTime, startTime)
                .le(Vital::getMeasureTime, endTime)
                .orderByAsc(Vital::getMeasureTime));

        Map<LocalDate, Integer> dayCountMap = new HashMap<>();
        for (int i = 0; i < safeDays; i += 1) {
            LocalDate day = startDay.plusDays(i);
            dayCountMap.put(day, 0);
        }
        for (Vital vital : weeklyVitals) {
            if (vital.getMeasureTime() == null) {
                continue;
            }
            LocalDate day = vital.getMeasureTime().toLocalDate();
            if (dayCountMap.containsKey(day)) {
                dayCountMap.put(day, dayCountMap.get(day) + 1);
            }
        }

        List<AdminDailyVitalCountVO> dailyVitalCounts = dayCountMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> AdminDailyVitalCountVO.builder()
                        .day(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        return AdminDailyStatsVO.builder()
                .todayPendingCount(pending)
                .todayDoneCount(done)
                .todaySkippedCount(skipped)
                .completionRate(completionRate)
                .weeklyVitalCount(weeklyVitals.size())
                .memberRates(memberRates)
                .dailyVitalCounts(dailyVitalCounts)
                .build();
    }

    private List<User> listFamilyMembers(Long familyId) {
        return userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getFamilyId, familyId)
                .orderByAsc(User::getCreateTime));
    }

    private List<Medicine> listFamilyMedicines(Long familyId) {
        return medicineService.list(new LambdaQueryWrapper<Medicine>()
                .eq(Medicine::getFamilyId, familyId));
    }

    private List<AdminPlanTodayItemVO> buildTodayPlanItems(Long familyId, Long userId, Integer status) {
        List<User> members = listFamilyMembers(familyId);
        if (members.isEmpty()) {
            return List.of();
        }

        Map<Long, User> userMap = members.stream().collect(Collectors.toMap(User::getId, item -> item));
        Set<Long> allowedUserIds = userMap.keySet();
        if (userId != null && !allowedUserIds.contains(userId)) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        String dayToken = String.valueOf(today.getDayOfWeek().getValue());
        Set<Long> targetUserIds = new HashSet<>();
        if (userId != null) {
            targetUserIds.add(userId);
        } else {
            targetUserIds.addAll(allowedUserIds);
        }

        List<MedicinePlan> plans = planService.list(new LambdaQueryWrapper<MedicinePlan>()
                .in(MedicinePlan::getUserId, targetUserIds)
                .eq(MedicinePlan::getStatus, MedicinePlan.STATUS_ENABLED)
                .le(MedicinePlan::getStartDate, today)
                .and(w -> w.isNull(MedicinePlan::getEndDate).or().ge(MedicinePlan::getEndDate, today)));

        List<MedicinePlan> validPlans = plans.stream()
                .filter(plan -> containsTakeDay(plan.getTakeDays(), dayToken))
                .collect(Collectors.toList());
        if (validPlans.isEmpty()) {
            return List.of();
        }

        Set<Long> medicineIds = validPlans.stream().map(MedicinePlan::getMedicineId).collect(Collectors.toSet());
        Map<Long, Medicine> medicineMap = medicineService.listByIds(medicineIds).stream()
                .collect(Collectors.toMap(Medicine::getId, item -> item));

        List<MedicineRecord> records = recordService.list(new LambdaQueryWrapper<MedicineRecord>()
                .in(MedicineRecord::getUserId, targetUserIds)
                .eq(MedicineRecord::getScheduledDate, today));
        Map<String, MedicineRecord> recordMap = records.stream()
                .collect(Collectors.toMap(
                        r -> buildRecordKey(r.getUserId(), r.getPlanId(), r.getSlotName()),
                        r -> r,
                        (a, b) -> a.getId() > b.getId() ? a : b
                ));

        List<AdminPlanTodayItemVO> items = new ArrayList<>();
        for (MedicinePlan plan : validPlans) {
            List<String> slots = parseSlots(plan.getRemindSlots());
            if (slots.isEmpty()) {
                continue;
            }

            for (String slot : slots) {
                MedicineRecord record = recordMap.get(buildRecordKey(plan.getUserId(), plan.getId(), slot));
                Integer recordStatus = record != null ? record.getStatus() : MedicineRecord.STATUS_PENDING;
                if (status != null && !status.equals(recordStatus)) {
                    continue;
                }

                Medicine medicine = medicineMap.get(plan.getMedicineId());
                User user = userMap.get(plan.getUserId());
                items.add(AdminPlanTodayItemVO.builder()
                        .recordId(record != null ? record.getId() : null)
                        .planId(plan.getId())
                        .userId(plan.getUserId())
                        .userName(user == null ? null : user.getNickname())
                        .medicineId(plan.getMedicineId())
                        .medicineName(medicine == null ? null : medicine.getName())
                        .medicineImageUrl(medicine == null ? null : medicine.getImageUrl())
                        .medicineStock(medicine == null ? null : medicine.getStock())
                        .medicineStockUnit(medicine == null ? null : medicine.getStockUnit())
                        .scheduledDate(today)
                        .slotName(slot)
                        .actualTime(record != null ? record.getActualTime() : null)
                        .recordStatus(recordStatus)
                        .planDosage(plan.getDosage())
                        .planRemindSlots(plan.getRemindSlots())
                        .planStartDate(plan.getStartDate())
                        .planEndDate(plan.getEndDate())
                        .planTakeDays(plan.getTakeDays())
                        .planStatus(plan.getStatus())
                        .build());
            }
        }

        items.sort(Comparator
                .comparing((AdminPlanTodayItemVO item) -> item.getUserName() == null ? "" : item.getUserName())
                .thenComparingInt(item -> slotWeight(item.getSlotName()))
                .thenComparing(AdminPlanTodayItemVO::getPlanId));
        return items;
    }

    private String buildRecordKey(Long userId, Long planId, String slotName) {
        return userId + "-" + planId + "-" + (slotName == null ? "" : slotName);
    }

    private boolean containsTakeDay(String takeDays, String dayToken) {
        if (takeDays == null || takeDays.isBlank()) {
            return true;
        }
        String normalized = "," + takeDays.replace(" ", "") + ",";
        return normalized.contains("," + dayToken + ",");
    }

    private List<String> parseSlots(String remindSlots) {
        if (remindSlots == null || remindSlots.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(remindSlots.split(","))
                .map(String::trim)
                .filter(slot -> !slot.isEmpty())
                .collect(Collectors.toList());
    }

    private int slotWeight(String slot) {
        if ("早".equals(slot)) {
            return 1;
        }
        if ("中".equals(slot)) {
            return 2;
        }
        if ("晚".equals(slot)) {
            return 3;
        }
        if ("睡前".equals(slot)) {
            return 4;
        }
        return 99;
    }
}

