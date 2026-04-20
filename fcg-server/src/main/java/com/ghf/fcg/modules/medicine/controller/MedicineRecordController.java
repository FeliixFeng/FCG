package com.ghf.fcg.modules.medicine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.dto.PageQuery;
import com.ghf.fcg.common.result.PageResult;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/medicine/record")
@RequiredArgsConstructor
@Tag(name = "服药记录模块", description = "服药记录管理")
public class MedicineRecordController {

    private final IMedicineRecordService recordService;
    private final IMedicinePlanService planService;
    private final IMedicineService medicineService;
    private final IUserService userService;

    @PostMapping
    @Operation(summary = "新增服药记录")
    public Result<Long> create(@RequestBody @Valid MedicineRecordCreateDTO dto) {
        Long currentUserId = UserContext.get().getUserId();
        requireFamilyId(currentUserId);

        MedicineRecord record = new MedicineRecord();
        record.setPlanId(dto.getPlanId());
        record.setUserId(dto.getUserId());
        record.setMedicineId(dto.getMedicineId());
        record.setScheduledDate(dto.getScheduledDate());
        record.setSlotName(dto.getSlotName());
        record.setActualTime(dto.getActualTime());
        record.setStatus(dto.getStatus() == null ? MedicineRecord.STATUS_PENDING : dto.getStatus());
        record.setRecordRemark(dto.getRecordRemark());

        recordService.save(record);
        reduceStockIfTaken(null, record.getStatus(), record.getMedicineId(), record.getPlanId());
        return Result.success(record.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新服药记录")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid MedicineRecordUpdateDTO dto) {
        Long currentUserId = UserContext.get().getUserId();
        requireFamilyId(currentUserId);

        MedicineRecord record = getRecord(id);
        Integer oldStatus = record.getStatus();
        applyRecordUpdate(record, dto);
        recordService.updateById(record);
        reduceStockIfTaken(oldStatus, record.getStatus(), record.getMedicineId(), record.getPlanId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除服药记录")
    public Result<Void> delete(@PathVariable Long id) {
        MedicineRecord record = getRecord(id);
        recordService.removeById(record.getId());
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "服药记录详情")
    public Result<MedicineRecordVO> get(@PathVariable Long id) {
        MedicineRecord record = getRecord(id);
        return Result.success(toRecordVO(record));
    }

    @GetMapping("/list")
    @Operation(summary = "服药记录列表")
    public Result<PageResult<MedicineRecordVO>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledDate,
            @ParameterObject PageQuery query) {
        Long currentUserId = UserContext.get().getUserId();

        LambdaQueryWrapper<MedicineRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicineRecord::getUserId, currentUserId);
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
        wrapper.orderByDesc(MedicineRecord::getScheduledDate);

        Page<MedicineRecord> pageResult = recordService.page(new Page<>(query.getPage(), query.getSize()), wrapper);
        return Result.success(PageResult.of(pageResult,
                pageResult.getRecords().stream().map(this::toRecordVO).collect(Collectors.toList())));
    }

    private Long requireFamilyId(Long userId) {
        User user = userService.getById(userId);
        if (user == null || user.getFamilyId() == null) {
            throw new BusinessException(MessageConstant.USER_NOT_IN_FAMILY);
        }
        return user.getFamilyId();
    }

    private MedicineRecord getRecord(Long id) {
        MedicineRecord record = recordService.getById(id);
        if (record == null) {
            throw new BusinessException(MessageConstant.RECORD_NOT_EXIST);
        }
        return record;
    }

    private void applyRecordUpdate(MedicineRecord record, MedicineRecordUpdateDTO dto) {
        if (dto.getScheduledDate() != null) {
            record.setScheduledDate(dto.getScheduledDate());
        }
        if (dto.getSlotName() != null) {
            record.setSlotName(dto.getSlotName());
        }
        if (dto.getActualTime() != null) {
            record.setActualTime(dto.getActualTime());
        }
        if (dto.getStatus() != null) {
            record.setStatus(dto.getStatus());
        }
        if (dto.getRecordRemark() != null) {
            record.setRecordRemark(dto.getRecordRemark());
        }
    }

    private MedicineRecordVO toRecordVO(MedicineRecord record) {
        return MedicineRecordVO.builder()
                .id(record.getId())
                .planId(record.getPlanId())
                .userId(record.getUserId())
                .medicineId(record.getMedicineId())
                .scheduledDate(record.getScheduledDate())
                .slotName(record.getSlotName())
                .actualTime(record.getActualTime())
                .status(record.getStatus())
                .recordRemark(record.getRecordRemark())
                .createTime(record.getCreateTime())
                .build();
    }

    private void reduceStockIfTaken(Integer oldStatus, Integer newStatus, Long medicineId, Long planId) {
        boolean nowTaken = newStatus != null && newStatus.equals(MedicineRecord.STATUS_TAKEN);
        boolean wasTaken = oldStatus != null && oldStatus.equals(MedicineRecord.STATUS_TAKEN);
        if (!nowTaken || wasTaken || medicineId == null) {
            return;
        }

        int deductCount = resolveDeductCount(planId);
        if (deductCount <= 0) {
            return;
        }

        Medicine medicine = medicineService.getById(medicineId);
        if (medicine == null || medicine.getStock() == null || medicine.getStock() <= 0) {
            return;
        }

        int remain = Math.max(medicine.getStock() - deductCount, 0);
        medicine.setStock(remain);
        medicineService.updateById(medicine);
    }

    private int resolveDeductCount(Long planId) {
        if (planId == null) {
            return 1;
        }
        MedicinePlan plan = planService.getById(planId);
        if (plan == null || plan.getDosage() == null) {
            return 1;
        }
        BigDecimal dosage = plan.getDosage();
        if (dosage.compareTo(BigDecimal.ZERO) <= 0) {
            return 1;
        }
        return Math.max(dosage.intValue(), 1);
    }
}
