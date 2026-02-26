package com.ghf.fcg.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 家庭账号登录请求 DTO
 */
@Data
@Schema(description = "家庭账号登录请求")
public class FamilyLoginDTO {

    @NotBlank(message = "账号不能为空")
    @Schema(description = "家庭账号")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "家庭密码")
    private String password;
}
