package com.ghf.fcg.modules.medicine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MedicineUpdateDTO {

    @Schema(description = "药品名称")
    private String name;

    @Schema(description = "规格")
    private String specification;

    @Schema(description = "生产厂家")
    private String manufacturer;

    @Schema(description = "剂型")
    private String dosageForm;

    @Schema(description = "药品图片URL")
    private String imageUrl;

    @Schema(description = "说明书")
    private String instructions;

    @Schema(description = "禁忌事项")
    private String contraindications;

    @Schema(description = "副作用")
    private String sideEffects;

    @Schema(description = "库存数量")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    @Schema(description = "库存单位")
    private String stockUnit;

    @Schema(description = "过期日期")
    private LocalDate expireDate;

    @Schema(description = "存放位置")
    private String storageLocation;
}
