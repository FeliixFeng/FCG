package com.ghf.fcg.modules.medicine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.context.UserContext;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.medicine.dto.MedicinePlanCreateDTO;
import com.ghf.fcg.modules.medicine.dto.MedicinePlanUpdateDTO;
import com.ghf.fcg.modules.medicine.entity.Medicine;
import com.ghf.fcg.modules.medicine.entity.MedicinePlan;
import com.ghf.fcg.modules.medicine.entity.MedicineRecord;
import com.ghf.fcg.modules.medicine.service.IMedicinePlanService;
import com.ghf.fcg.modules.medicine.service.IMedicineRecordService;
import com.ghf.fcg.modules.medicine.service.IMedicineService;
import com.ghf.fcg.modules.medicine.vo.MedicinePlanRecordVO;
import com.ghf.fcg.modules.medicine.vo.MedicinePlanVO;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/medicine/plan")
@RequiredArgsConstructor
@Tag(name = "用药计划模块", description = "用药计划管理与计划记录联表查询")
public class MedicinePlanController {

    private final IMedicinePlanService planService;
    private final IMedicineRecordService recordService;
    private final IMedicineService medicineService;
    private final IUserService userService;

    @PostMapping
    @Operation(summary = "新增用药计划")
    public Result<Long> create(@RequestBody @Valid MedicinePlanCreateDTO dto) {
        Long userId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(userId);
        validateFamilyUser(familyId, dto.getUserId());
        validateFamilyMedicine(familyId, dto.getMedicineId());

        MedicinePlan plan = new MedicinePlan();
        plan.setUserId(dto.getUserId());
        plan.setFamilyId(familyId);
        plan.setMedicineId(dto.getMedicineId());
        plan.setDosage(dto.getDosage());
        plan.setFrequency(dto.getFrequency());
        plan.setRemindTimes(dto.getRemindTimes());
        plan.setStartDate(dto.getStartDate());
        plan.setEndDate(dto.getEndDate());
        plan.setTakeDays(dto.getTakeDays() == null ? "1,2,3,4,5,6,7" : dto.getTakeDays());
        plan.setNotes(dto.getNotes());
        plan.setStatus(dto.getStatus() == null ? MedicinePlan.STATUS_ENABLED : dto.getStatus());

        planService.save(plan);
        return Result.success(plan.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用药计划")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid MedicinePlanUpdateDTO dto) {
        Long userId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(userId);

        MedicinePlan plan = getFamilyPlan(id, familyId);
        applyPlanUpdate(plan, familyId, dto);
        planService.updateById(plan);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用药计划")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(userId);

        MedicinePlan plan = getFamilyPlan(id, familyId);
        planService.removeById(plan.getId());
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "用药计划详情")
    public Result<MedicinePlanVO> get(@PathVariable Long id) {
        Long userId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(userId);

        MedicinePlan plan = getFamilyPlan(id, familyId);
        return Result.success(toPlanVO(plan));
    }

    @GetMapping("/list")
    @Operation(summary = "用药计划列表")
    public Result<List<MedicinePlanVO>> list(@RequestParam(required = false) Long userId,
                                             @RequestParam(required = false) Integer status) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);

        LambdaQueryWrapper<MedicinePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicinePlan::getFamilyId, familyId);
        if (userId != null) {
            wrapper.eq(MedicinePlan::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(MedicinePlan::getStatus, status);
        }
        wrapper.orderByDesc(MedicinePlan::getCreateTime);

        List<MedicinePlanVO> list = planService.list(wrapper)
                .stream()
                .map(this::toPlanVO)
                .collect(Collectors.toList());
        return Result.success(list);
    }

    @GetMapping("/records")
    @Operation(summary = "计划记录联表查询")
    public Result<List<MedicinePlanRecordVO>> listPlanRecords(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledDate) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);

        LambdaQueryWrapper<MedicineRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicineRecord::getFamilyId, familyId);
        if (userId != null) {
            wrapper.eq(MedicineRecord::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(MedicineRecord::getStatus, status);
        }
        if (scheduledDate != null) {
            wrapper.eq(MedicineRecord::getScheduledDate, scheduledDate);
        }
        wrapper.orderByDesc(MedicineRecord::getScheduledDate)
                .orderByDesc(MedicineRecord::getScheduledTime);

        List<MedicineRecord> records = recordService.list(wrapper);
        if (records.isEmpty()) {
            return Result.success(List.of());
        }

        Set<Long> planIds = records.stream().map(MedicineRecord::getPlanId).collect(Collectors.toSet());
        Map<Long, MedicinePlan> planMap = planService.listByIds(planIds).stream()
                .collect(Collectors.toMap(MedicinePlan::getId, item -> item));

        Set<Long> medicineIds = records.stream().map(MedicineRecord::getMedicineId).collect(Collectors.toSet());
        Map<Long, Medicine> medicineMap = medicineService.listByIds(medicineIds).stream()
                .collect(Collectors.toMap(Medicine::getId, item -> item));

        List<MedicinePlanRecordVO> list = new ArrayList<>();
        for (MedicineRecord record : records) {
            MedicinePlan plan = planMap.get(record.getPlanId());
            Medicine medicine = medicineMap.get(record.getMedicineId());
            if (plan == null) {
                continue;
            }
            list.add(MedicinePlanRecordVO.builder()
                    .recordId(record.getId())
                    .planId(record.getPlanId())
                    .userId(record.getUserId())
                    .medicineId(record.getMedicineId())
                    .medicineName(medicine == null ? null : medicine.getName())
                    .scheduledDate(record.getScheduledDate())
                    .scheduledTime(record.getScheduledTime())
                    .actualTime(record.getActualTime())
                    .recordStatus(record.getStatus())
                    .recordNotes(record.getNotes())
                    .planDosage(plan.getDosage())
                    .planFrequency(plan.getFrequency())
                    .planRemindTimes(plan.getRemindTimes())
                    .planStartDate(plan.getStartDate())
                    .planEndDate(plan.getEndDate())
                    .planTakeDays(plan.getTakeDays())
                    .planNotes(plan.getNotes())
                    .planStatus(plan.getStatus())
                    .build());
        }
        return Result.success(list);
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

    private void validateFamilyMedicine(Long familyId, Long medicineId) {
        Medicine medicine = medicineService.getById(medicineId);
        if (medicine == null || !familyId.equals(medicine.getFamilyId())) {
            throw new BusinessException(MessageConstant.MEDICINE_FAMILY_MISMATCH);
        }
    }

    private MedicinePlan getFamilyPlan(Long id, Long familyId) {
        MedicinePlan plan = planService.getById(id);
        if (plan == null || !familyId.equals(plan.getFamilyId())) {
            throw new BusinessException(MessageConstant.PLAN_NOT_EXIST);
        }
        return plan;
    }

    private void applyPlanUpdate(MedicinePlan plan, Long familyId, MedicinePlanUpdateDTO dto) {
        if (dto.getUserId() != null) {
            validateFamilyUser(familyId, dto.getUserId());
            plan.setUserId(dto.getUserId());
        }
        if (dto.getMedicineId() != null) {
            validateFamilyMedicine(familyId, dto.getMedicineId());
            plan.setMedicineId(dto.getMedicineId());
        }
        if (dto.getDosage() != null) {
            plan.setDosage(dto.getDosage());
        }
        if (dto.getFrequency() != null) {
            plan.setFrequency(dto.getFrequency());
        }
        if (dto.getRemindTimes() != null) {
            plan.setRemindTimes(dto.getRemindTimes());
        }
        if (dto.getStartDate() != null) {
            plan.setStartDate(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            plan.setEndDate(dto.getEndDate());
        }
        if (dto.getTakeDays() != null) {
            plan.setTakeDays(dto.getTakeDays());
        }
        if (dto.getNotes() != null) {
            plan.setNotes(dto.getNotes());
        }
        if (dto.getStatus() != null) {
            plan.setStatus(dto.getStatus());
        }
    }

    private MedicinePlanVO toPlanVO(MedicinePlan plan) {
        return MedicinePlanVO.builder()
                .id(plan.getId())
                .userId(plan.getUserId())
                .familyId(plan.getFamilyId())
                .medicineId(plan.getMedicineId())
                .dosage(plan.getDosage())
                .frequency(plan.getFrequency())
                .remindTimes(plan.getRemindTimes())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .takeDays(plan.getTakeDays())
                .notes(plan.getNotes())
                .status(plan.getStatus())
                .createTime(plan.getCreateTime())
                .build();
    }
}
