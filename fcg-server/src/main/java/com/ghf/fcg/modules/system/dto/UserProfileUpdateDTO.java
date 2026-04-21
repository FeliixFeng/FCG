package com.ghf.fcg.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "成员自助更新健康档案请求")
public class UserProfileUpdateDTO {

    @Schema(description = "出生日期")
    private LocalDate birthday;

    @Schema(description = "身高(cm)")
    private BigDecimal height;

    @Schema(description = "体重(kg)")
    private BigDecimal weight;

    @Schema(description = "病史")
    private String disease;

    @Schema(description = "过敏史")
    private String allergy;

    @Schema(description = "备注")
    private String remark;
}
