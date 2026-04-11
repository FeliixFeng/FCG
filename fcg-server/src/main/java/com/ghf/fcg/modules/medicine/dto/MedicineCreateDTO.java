package com.ghf.fcg.modules.medicine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MedicineCreateDTO {

    @Schema(description = "药品名称")
    @NotBlank(message = "药品名称不能为空")
    private String name;

    @Schema(description = "规格（如 0.3g*20粒）")
    private String specification;

    @Schema(description = "封面图片URL")
    private String imageUrl;

    @Schema(description = "库存数量")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    @Schema(description = "库存单位（片/粒/ml）")
    private String stockUnit;

    @Schema(description = "有效期")
    private LocalDate expireDate;

    @Schema(description = "用药注意")
    private String usageNotes;
}