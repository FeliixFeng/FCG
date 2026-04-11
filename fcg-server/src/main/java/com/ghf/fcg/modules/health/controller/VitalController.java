package com.ghf.fcg.modules.health.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.dto.PageQuery;
import com.ghf.fcg.common.result.PageResult;
import com.ghf.fcg.common.context.UserContext;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.health.dto.VitalCreateDTO;
import com.ghf.fcg.modules.health.dto.VitalUpdateDTO;
import com.ghf.fcg.modules.health.entity.Vital;
import com.ghf.fcg.modules.health.service.IVitalService;
import com.ghf.fcg.modules.health.vo.VitalVO;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/health/vital")
@RequiredArgsConstructor
@Tag(name = "体征模块", description = "体征数据采集与查询")
public class VitalController {

    private final IVitalService vitalService;
    private final IUserService userService;

    @PostMapping
    @Operation(summary = "新增体征记录")
    public Result<Long> create(@RequestBody @Valid VitalCreateDTO dto) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);
        validateFamilyUser(familyId, dto.getUserId());

        Vital vital = new Vital();
        vital.setUserId(dto.getUserId());
        vital.setFamilyId(familyId);
        vital.setType(dto.getType());
        vital.setValueSystolic(dto.getValueSystolic());
        vital.setValueDiastolic(dto.getValueDiastolic());
        vital.setValue(dto.getValue());
        vital.setUnit(dto.getUnit());
        vital.setMeasureTime(dto.getMeasureTime());
        vital.setMeasurePoint(dto.getMeasurePoint());
        vital.setNotes(dto.getNotes());

        vitalService.save(vital);
        return Result.success(vital.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新体征记录")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid VitalUpdateDTO dto) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);

        Vital vital = getFamilyVital(id, familyId);
        applyVitalUpdate(vital, familyId, dto);
        vitalService.updateById(vital);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除体征记录")
    public Result<Void> delete(@PathVariable Long id) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);

        Vital vital = getFamilyVital(id, familyId);
        vitalService.removeById(vital.getId());
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "体征记录详情")
    public Result<VitalVO> get(@PathVariable Long id) {
        Long currentUserId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(currentUserId);

        Vital vital = getFamilyVital(id, familyId);
        return Result.success(toVitalVO(vital));
    }

    @GetMapping("/list")
    @Operation(summary = "体征记录列表")
    public Result<PageResult<VitalVO>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @ParameterObject PageQuery query) {
        Long currentUserId = UserContext.get().getUserId();
        
        Vital sampleVital = vitalService.getOne(new LambdaQueryWrapper<Vital>()
                .eq(Vital::getUserId, currentUserId)
                .last("LIMIT 1"));
        
        Long familyId = sampleVital != null ? sampleVital.getFamilyId() : null;
        if (familyId == null) {
            familyId = requireFamilyId(currentUserId);
        }

        Long targetUserId = userId;
        if (targetUserId == null) {
            targetUserId = currentUserId;
        }

        LambdaQueryWrapper<Vital> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Vital::getFamilyId, familyId)
                .eq(Vital::getUserId, targetUserId);
        if (type != null) {
            wrapper.eq(Vital::getType, type);
        }
        if (startTime != null) {
            wrapper.ge(Vital::getMeasureTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Vital::getMeasureTime, endTime);
        }
        wrapper.orderByAsc(Vital::getMeasureTime);

        Page<Vital> pageResult = vitalService.page(new Page<>(query.getPage(), query.getSize()), wrapper);
        return Result.success(PageResult.of(pageResult,
                pageResult.getRecords().stream().map(this::toVitalVO).collect(Collectors.toList())));
    }

    @GetMapping("/weekly")
    @Operation(summary = "近一周体征记录")
    public Result<List<VitalVO>> weekly(@RequestParam(required = false) Long userId,
                                        @RequestParam(required = false) Integer type) {
        Long currentUserId = UserContext.get().getUserId();
        
        Vital sampleVital = vitalService.getOne(new LambdaQueryWrapper<Vital>()
                .eq(Vital::getUserId, currentUserId)
                .last("LIMIT 1"));
        
        Long familyId = sampleVital != null ? sampleVital.getFamilyId() : null;
        if (familyId == null) {
            familyId = requireFamilyId(currentUserId);
        }

        Long targetUserId = userId;
        if (targetUserId == null) {
            targetUserId = currentUserId;
        }

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(7);

        LambdaQueryWrapper<Vital> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Vital::getFamilyId, familyId)
                .eq(Vital::getUserId, targetUserId)
                .ge(Vital::getMeasureTime, startTime)
                .le(Vital::getMeasureTime, endTime);
        if (type != null) {
            wrapper.eq(Vital::getType, type);
        }
        wrapper.orderByAsc(Vital::getMeasureTime);

        List<VitalVO> list = vitalService.list(wrapper)
                .stream()
                .map(this::toVitalVO)
                .collect(Collectors.toList());
        return Result.success(list);
    }

    @GetMapping("/today")
    @Operation(summary = "今日最新体征", description = "返回每种类型的最新一条记录")
    public Result<List<VitalVO>> today(@RequestParam(required = false) Long userId) {
        Long currentUserId = UserContext.get().getUserId();
        
        Vital sampleVital = vitalService.getOne(new LambdaQueryWrapper<Vital>()
                .eq(Vital::getUserId, currentUserId)
                .last("LIMIT 1"));
        
        Long familyId = sampleVital != null ? sampleVital.getFamilyId() : null;
        if (familyId == null) {
            familyId = requireFamilyId(currentUserId);
        }

        Long targetUserId = userId;
        if (targetUserId == null) {
            targetUserId = currentUserId;
        }

        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now();

        List<VitalVO> result = new java.util.ArrayList<>();

        for (int type = 1; type <= 3; type++) {
            LambdaQueryWrapper<Vital> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Vital::getFamilyId, familyId)
                    .eq(Vital::getUserId, targetUserId)
                    .eq(Vital::getType, type)
                    .ge(Vital::getMeasureTime, todayStart)
                    .le(Vital::getMeasureTime, todayEnd)
                    .orderByDesc(Vital::getMeasureTime)
                    .last("LIMIT 1");

            Vital vital = vitalService.getOne(wrapper);
            if (vital != null) {
                result.add(toVitalVO(vital));
            }
        }

        return Result.success(result);
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

    private Vital getFamilyVital(Long id, Long familyId) {
        Vital vital = vitalService.getById(id);
        if (vital == null || !familyId.equals(vital.getFamilyId())) {
            throw new BusinessException(MessageConstant.VITAL_NOT_EXIST);
        }
        return vital;
    }

    private void applyVitalUpdate(Vital vital, Long familyId, VitalUpdateDTO dto) {
        if (dto.getUserId() != null) {
            validateFamilyUser(familyId, dto.getUserId());
            vital.setUserId(dto.getUserId());
        }
        if (dto.getType() != null) {
            vital.setType(dto.getType());
        }
        if (dto.getValueSystolic() != null) {
            vital.setValueSystolic(dto.getValueSystolic());
        }
        if (dto.getValueDiastolic() != null) {
            vital.setValueDiastolic(dto.getValueDiastolic());
        }
        if (dto.getValue() != null) {
            vital.setValue(dto.getValue());
        }
        if (dto.getUnit() != null) {
            vital.setUnit(dto.getUnit());
        }
        if (dto.getMeasureTime() != null) {
            vital.setMeasureTime(dto.getMeasureTime());
        }
        if (dto.getMeasurePoint() != null) {
            vital.setMeasurePoint(dto.getMeasurePoint());
        }
        if (dto.getNotes() != null) {
            vital.setNotes(dto.getNotes());
        }
    }

    private VitalVO toVitalVO(Vital vital) {
        return VitalVO.builder()
                .id(vital.getId())
                .userId(vital.getUserId())
                .familyId(vital.getFamilyId())
                .type(vital.getType())
                .valueSystolic(vital.getValueSystolic())
                .valueDiastolic(vital.getValueDiastolic())
                .value(vital.getValue())
                .unit(vital.getUnit())
                .measureTime(vital.getMeasureTime())
                .measurePoint(vital.getMeasurePoint())
                .notes(vital.getNotes())
                .createTime(vital.getCreateTime())
                .build();
    }
}
