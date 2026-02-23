package com.ghf.fcg.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserRegisterDTO {

    @Schema(description = "用户名(4-20位字母数字下划线)")
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名4-20位字母数字下划线")
    private String username;

    @Schema(description = "密码(6-20位)")
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码6-20位")
    private String password;

    @Schema(description = "昵称")
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;
}
