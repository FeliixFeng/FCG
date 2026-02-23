package com.ghf.fcg.modules.medicine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghf.fcg.common.context.UserContext;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.medicine.dto.MedicineRecordCreateDTO;
import com.ghf.fcg.modules.medicine.dto.MedicineRecordUpdateDTO;
import com.ghf.fcg.modules.medicine.entity.Medicine;
import com.ghf.fcg.modules.medicine.entity.MedicinePlan;
import com.ghf.fcg.modules.medicine.entity.MedicineRecord;
import com.ghf.fcg.modules.medicine.service.IMedicinePlanService;
import com.ghf.fcg.modules.medicine.service.IMedicineRecordService;
import com.ghf.fcg.modules.medicine.service.IMedicineService;
import com.ghf.fcg.modules.medicine.vo.MedicineRecordVO;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/medicine/record")
@RequiredArgsConstructor
public class MedicineRecordController {

    private final IMedicineRecordService recordService;
    private final IMedicinePlanService planService;
    private final IMedicineService medicineService;
    private final IUserService userService;

    @PostMapping
    @Operation(summary = "新增服药记录")
    public Result<Long> create(@RequestBody @Valid MedicineRecordCreateDTO dto) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);

        MedicinePlan plan = validatePlan(familyId, dto.getPlanId());
        validateFamilyUser(familyId, dto.getUserId());
        validateFamilyMedicine(familyId, dto.getMedicineId());
        ensurePlanRelation(plan, dto.getUserId(), dto.getMedicineId());

        MedicineRecord record = new MedicineRecord();
        record.setPlanId(dto.getPlanId());
        record.setUserId(dto.getUserId());
        record.setFamilyId(familyId);
        record.setMedicineId(dto.getMedicineId());
        record.setScheduledDate(dto.getScheduledDate());
        record.setScheduledTime(dto.getScheduledTime());
        record.setActualTime(dto.getActualTime());
        record.setStatus(dto.getStatus() == null ? MedicineRecord.STATUS_PENDING : dto.getStatus());
        record.setNotes(dto.getNotes());

        recordService.save(record);
        return Result.success(record.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新服药记录")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid MedicineRecordUpdateDTO dto) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);

        MedicineRecord record = getFamilyRecord(id, familyId);
        applyRecordUpdate(record, familyId, dto);
        recordService.updateById(record);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除服药记录")
    public Result<Void> delete(@PathVariable Long id) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);

        MedicineRecord record = getFamilyRecord(id, familyId);
        recordService.removeById(record.getId());
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "服药记录详情")
    public Result<MedicineRecordVO> get(@PathVariable Long id) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);

        MedicineRecord record = getFamilyRecord(id, familyId);
        return Result.success(toRecordVO(record));
    }

    @GetMapping("/list")
    @Operation(summary = "服药记录列表")
    public Result<List<MedicineRecordVO>> list(@RequestParam(required = false) Long userId,
                                               @RequestParam(required = false) Long planId,
                                               @RequestParam(required = false) Integer status,
                                               @RequestParam(required = false)
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledDate) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);

        LambdaQueryWrapper<MedicineRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicineRecord::getFamilyId, familyId);
        if (userId != null) {
            wrapper.eq(MedicineRecord::getUserId, userId);
        }
        if (planId != null) {
            wrapper.eq(MedicineRecord::getPlanId, planId);
        }
        if (status != null) {
            wrapper.eq(MedicineRecord::getStatus, status);
        }
        if (scheduledDate != null) {
            wrapper.eq(MedicineRecord::getScheduledDate, scheduledDate);
        }
        wrapper.orderByDesc(MedicineRecord::getScheduledDate)
                .orderByDesc(MedicineRecord::getScheduledTime);

        List<MedicineRecordVO> list = recordService.list(wrapper)
                .stream()
                .map(this::toRecordVO)
                .collect(Collectors.toList());
        return Result.success(list);
    }

    private Long requireFamilyId(Long userId) {
        User user = userService.getById(userId);
        if (user == null || user.getFamilyId() == null) {
            throw new BusinessException("用户未加入家庭");
        }
        return user.getFamilyId();
    }

    private void validateFamilyUser(Long familyId, Long userId) {
        User user = userService.getById(userId);
        if (user == null || !familyId.equals(user.getFamilyId())) {
            throw new BusinessException("使用者不存在或不属于当前家庭");
        }
    }

    private void validateFamilyMedicine(Long familyId, Long medicineId) {
        Medicine medicine = medicineService.getById(medicineId);
        if (medicine == null || !familyId.equals(medicine.getFamilyId())) {
            throw new BusinessException("药品不存在或不属于当前家庭");
        }
    }

    private MedicinePlan validatePlan(Long familyId, Long planId) {
        MedicinePlan plan = planService.getById(planId);
        if (plan == null || !familyId.equals(plan.getFamilyId())) {
            throw new BusinessException("用药计划不存在或不属于当前家庭");
        }
        return plan;
    }

    private void ensurePlanRelation(MedicinePlan plan, Long userId, Long medicineId) {
        if (!plan.getUserId().equals(userId)) {
            throw new BusinessException("记录使用者与计划不一致");
        }
        if (!plan.getMedicineId().equals(medicineId)) {
            throw new BusinessException("记录药品与计划不一致");
        }
    }

    private MedicineRecord getFamilyRecord(Long id, Long familyId) {
        MedicineRecord record = recordService.getById(id);
        if (record == null || !familyId.equals(record.getFamilyId())) {
            throw new BusinessException("服药记录不存在");
        }
        return record;
    }

    private void applyRecordUpdate(MedicineRecord record, Long familyId, MedicineRecordUpdateDTO dto) {
        Long targetPlanId = dto.getPlanId() == null ? record.getPlanId() : dto.getPlanId();
        Long targetUserId = dto.getUserId() == null ? record.getUserId() : dto.getUserId();
        Long targetMedicineId = dto.getMedicineId() == null ? record.getMedicineId() : dto.getMedicineId();

        MedicinePlan plan = validatePlan(familyId, targetPlanId);
        validateFamilyUser(familyId, targetUserId);
        validateFamilyMedicine(familyId, targetMedicineId);
        ensurePlanRelation(plan, targetUserId, targetMedicineId);

        record.setPlanId(targetPlanId);
        record.setUserId(targetUserId);
        record.setMedicineId(targetMedicineId);

        if (dto.getScheduledDate() != null) {
            record.setScheduledDate(dto.getScheduledDate());
        }
        if (dto.getScheduledTime() != null) {
            record.setScheduledTime(dto.getScheduledTime());
        }
        if (dto.getActualTime() != null) {
            record.setActualTime(dto.getActualTime());
        }
        if (dto.getStatus() != null) {
            record.setStatus(dto.getStatus());
        }
        if (dto.getNotes() != null) {
            record.setNotes(dto.getNotes());
        }
    }

    private MedicineRecordVO toRecordVO(MedicineRecord record) {
        return MedicineRecordVO.builder()
                .id(record.getId())
                .planId(record.getPlanId())
                .userId(record.getUserId())
                .familyId(record.getFamilyId())
                .medicineId(record.getMedicineId())
                .scheduledDate(record.getScheduledDate())
                .scheduledTime(record.getScheduledTime())
                .actualTime(record.getActualTime())
                .status(record.getStatus())
                .notes(record.getNotes())
                .createTime(record.getCreateTime())
                .build();
    }
}
