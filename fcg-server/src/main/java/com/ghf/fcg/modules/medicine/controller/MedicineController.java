package com.ghf.fcg.modules.medicine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.dto.PageQuery;
import com.ghf.fcg.common.result.PageResult;
import com.ghf.fcg.common.context.UserContext;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.ai.service.AiService;
import com.ghf.fcg.modules.medicine.dto.MedicineCreateDTO;
import com.ghf.fcg.modules.medicine.dto.MedicineUpdateDTO;
import com.ghf.fcg.modules.medicine.entity.Medicine;
import com.ghf.fcg.modules.medicine.service.IMedicineService;
import com.ghf.fcg.modules.medicine.vo.MedicineOcrEnhancedVO;
import com.ghf.fcg.modules.medicine.vo.MedicineOcrParsedVO;
import com.ghf.fcg.modules.medicine.vo.MedicineVO;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/medicine")
@RequiredArgsConstructor
@Tag(name = "药品模块", description = "药品基础信息管理")
public class MedicineController {

    private final IMedicineService medicineService;
    private final IUserService userService;
    private final AiService aiService;

    @PostMapping
    @Operation(summary = "新增药品")
    public Result<Long> create(@RequestBody @Valid MedicineCreateDTO dto) {
        Long userId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(userId);

        Medicine medicine = new Medicine();
        medicine.setFamilyId(familyId);
        medicine.setName(dto.getName());
        medicine.setSpecification(dto.getSpecification());
        medicine.setManufacturer(dto.getManufacturer());
        medicine.setDosageForm(dto.getDosageForm());
        medicine.setImageUrl(dto.getImageUrl());
        medicine.setInstructions(dto.getInstructions());
        medicine.setContraindications(dto.getContraindications());
        medicine.setSideEffects(dto.getSideEffects());
        medicine.setStock(dto.getStock());
        medicine.setStockUnit(dto.getStockUnit());
        medicine.setExpireDate(dto.getExpireDate());
        medicine.setStorageLocation(dto.getStorageLocation());

        medicineService.save(medicine);
        return Result.success(medicine.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新药品")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid MedicineUpdateDTO dto) {
        Long userId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(userId);

        Medicine medicine = getFamilyMedicine(id, familyId);
        applyMedicineUpdate(medicine, dto);
        medicineService.updateById(medicine);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除药品")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(userId);

        Medicine medicine = getFamilyMedicine(id, familyId);
        medicineService.removeById(medicine.getId());
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "药品详情")
    public Result<MedicineVO> get(@PathVariable Long id) {
        Long userId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(userId);

        Medicine medicine = getFamilyMedicine(id, familyId);
        return Result.success(toMedicineVO(medicine));
    }

    @GetMapping("/list")
    @Operation(summary = "药品列表")
    public Result<PageResult<MedicineVO>> list(@ParameterObject PageQuery query) {
        Long userId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(userId);

        LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Medicine::getFamilyId, familyId)
                .orderByDesc(Medicine::getCreateTime);
        Page<Medicine> pageResult = medicineService.page(new Page<>(query.getPage(), query.getSize()), wrapper);
        return Result.success(PageResult.of(pageResult,
                pageResult.getRecords().stream().map(this::toMedicineVO).collect(Collectors.toList())));
    }

    @PostMapping("/ocr")
    @Operation(summary = "药品图片智能识别（多模态AI一步完成）")
    public Result<MedicineOcrEnhancedVO> ocr(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(MessageConstant.PARAM_ERROR);
        }

        // 1. 转换为Base64
        String imageBase64;
        try {
            imageBase64 = Base64.getEncoder().encodeToString(file.getBytes());
        } catch (Exception e) {
            throw new BusinessException("图片读取失败");
        }

        // 2. 调用多模态AI识别（OCR + 结构化一步完成）
        String aiRaw = null;
        MedicineOcrParsedVO parsed = null;
        boolean fallback = false;
        try {
            aiRaw = aiService.recognizeMedicineImage(imageBase64);
            parsed = aiService.parseMedicineInfo(aiRaw);
        } catch (RuntimeException e) {
            fallback = true;
        }

        // 3. 返回结果（不再包含rawText、rawLines、ocrError）
        MedicineOcrEnhancedVO result = MedicineOcrEnhancedVO.builder()
                .parsed(parsed)
                .model(aiService.currentModel())
                .fallback(fallback)
                .aiRaw(aiRaw)
                .build();

        return Result.success(result);
    }

    private Long requireFamilyId(Long userId) {
        User user = userService.getById(userId);
        if (user == null || user.getFamilyId() == null) {
            throw new BusinessException(MessageConstant.USER_NOT_IN_FAMILY);
        }
        return user.getFamilyId();
    }

    private Medicine getFamilyMedicine(Long id, Long familyId) {
        Medicine medicine = medicineService.getById(id);
        if (medicine == null || !familyId.equals(medicine.getFamilyId())) {
            throw new BusinessException(MessageConstant.MEDICINE_NOT_EXIST);
        }
        return medicine;
    }

    private void applyMedicineUpdate(Medicine medicine, MedicineUpdateDTO dto) {
        if (dto.getName() != null) {
            medicine.setName(dto.getName());
        }
        if (dto.getSpecification() != null) {
            medicine.setSpecification(dto.getSpecification());
        }
        if (dto.getManufacturer() != null) {
            medicine.setManufacturer(dto.getManufacturer());
        }
        if (dto.getDosageForm() != null) {
            medicine.setDosageForm(dto.getDosageForm());
        }
        if (dto.getImageUrl() != null) {
            medicine.setImageUrl(dto.getImageUrl());
        }
        if (dto.getInstructions() != null) {
            medicine.setInstructions(dto.getInstructions());
        }
        if (dto.getContraindications() != null) {
            medicine.setContraindications(dto.getContraindications());
        }
        if (dto.getSideEffects() != null) {
            medicine.setSideEffects(dto.getSideEffects());
        }
        if (dto.getStock() != null) {
            medicine.setStock(dto.getStock());
        }
        if (dto.getStockUnit() != null) {
            medicine.setStockUnit(dto.getStockUnit());
        }
        if (dto.getExpireDate() != null) {
            medicine.setExpireDate(dto.getExpireDate());
        }
        if (dto.getStorageLocation() != null) {
            medicine.setStorageLocation(dto.getStorageLocation());
        }
    }

    private MedicineVO toMedicineVO(Medicine medicine) {
        return MedicineVO.builder()
                .id(medicine.getId())
                .familyId(medicine.getFamilyId())
                .name(medicine.getName())
                .specification(medicine.getSpecification())
                .manufacturer(medicine.getManufacturer())
                .dosageForm(medicine.getDosageForm())
                .imageUrl(medicine.getImageUrl())
                .instructions(medicine.getInstructions())
                .contraindications(medicine.getContraindications())
                .sideEffects(medicine.getSideEffects())
                .stock(medicine.getStock())
                .stockUnit(medicine.getStockUnit())
                .expireDate(medicine.getExpireDate())
                .storageLocation(medicine.getStorageLocation())
                .createTime(medicine.getCreateTime())
                .build();
    }
}
