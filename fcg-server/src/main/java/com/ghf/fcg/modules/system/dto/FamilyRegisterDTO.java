package com.ghf.fcg.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 家庭注册请求 DTO
 * 注册时创建家庭账号，同时添加第一个成员（自动成为管理员）
 */
@Data
@Schema(description = "家庭注册请求")
public class FamilyRegisterDTO {

    @Schema(description = "家庭名称，不填则默认为账号名+的家")
    private String familyName;

    @NotBlank(message = "家庭账号不能为空")
    @Size(min = 4, max = 20, message = "账号长度4-20位")
    @Schema(description = "家庭账号（登录用，全局唯一）")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    @Schema(description = "家庭密码")
    private String password;

    @NotBlank(message = "管理员昵称不能为空")
    @Schema(description = "第一个成员（管理员）的昵称")
    private String adminNickname;
}
