package com.ghf.fcg.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "管理员更新成员请求")
public class MemberUpdateDTO {

    @Schema(description = "昵称")
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "家庭关系")
    private String relation;

    @Schema(description = "角色：0-管理员 1-普通成员 2-受控成员")
    private Integer role;

    private LocalDate birthday;
    private BigDecimal height;
    private BigDecimal weight;
    private String disease;
    private String allergy;
    private String remark;
}
