package com.ghf.fcg.modules.medicine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.result.PageResult;
import com.ghf.fcg.common.context.UserContext;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.medicine.dto.MedicineCreateDTO;
import com.ghf.fcg.modules.medicine.dto.MedicineUpdateDTO;
import com.ghf.fcg.modules.medicine.entity.Medicine;
import com.ghf.fcg.modules.medicine.service.IMedicineService;
import com.ghf.fcg.modules.medicine.vo.MedicineOcrVO;
import com.ghf.fcg.modules.medicine.vo.MedicineVO;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/medicine")
@RequiredArgsConstructor
@Tag(name = "药品模块", description = "药品基础信息管理")
public class MedicineController {

    private final IMedicineService medicineService;
    private final IUserService userService;

    @Value("${ocr.base-url}")
    private String ocrBaseUrl;

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
    public Result<PageResult<MedicineVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        Long userId = UserContext.get().getUserId();
        Long familyId = requireFamilyId(userId);

        LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Medicine::getFamilyId, familyId)
                .orderByDesc(Medicine::getCreateTime);
        Page<Medicine> pageResult = medicineService.page(new Page<>(page, size), wrapper);
        return Result.success(PageResult.of(pageResult,
                pageResult.getRecords().stream().map(this::toMedicineVO).collect(Collectors.toList())));
    }

    @PostMapping("/ocr")
    @Operation(summary = "药品图片OCR识别")
    public Result<MedicineOcrVO> ocr(@RequestPart("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BusinessException(MessageConstant.PARAM_ERROR);
        }
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (MultipartFile file : files) {
            try {
                ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }
                };
                body.add("files", resource);
            } catch (Exception e) {
                throw new BusinessException(MessageConstant.OCR_FAILED);
            }
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        try {
            MedicineOcrVO response = restTemplate.postForObject(ocrBaseUrl + "/smart-ocr", requestEntity, MedicineOcrVO.class);
            return Result.success(response);
        } catch (RestClientException e) {
            throw new BusinessException(MessageConstant.OCR_FAILED);
        }
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
