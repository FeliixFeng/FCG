package com.ghf.fcg.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserRegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名4-20位字母数字下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码6-20位")
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String phone;

    private String inviteCode;
}
