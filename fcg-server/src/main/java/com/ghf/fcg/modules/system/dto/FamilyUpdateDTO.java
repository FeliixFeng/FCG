package com.ghf.fcg.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class FamilyUpdateDTO {

    @NotBlank(message = "家庭名称不能为空")
    @Size(max = 30, message = "家庭名称最多 30 字")
    private String familyName;

    @NotNull(message = "低库存阈值不能为空")
    @Min(value = 1, message = "低库存阈值最小为 1")
    @Max(value = 999, message = "低库存阈值最大为 999")
    private Integer lowStockThreshold;

    @NotNull(message = "临期提醒天数不能为空")
    @Min(value = 1, message = "临期提醒天数最小为 1")
    @Max(value = 365, message = "临期提醒天数最大为 365")
    private Integer expiringDays;
}
