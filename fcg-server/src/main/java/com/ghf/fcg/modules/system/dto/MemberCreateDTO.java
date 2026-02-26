package com.ghf.fcg.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加家庭成员请求 DTO
 * 只有管理员可以调用此接口
 */
@Data
@Schema(description = "添加家庭成员请求")
public class MemberCreateDTO {

    @NotBlank(message = "昵称不能为空")
    @Schema(description = "成员昵称（选人页展示用）")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像URL（OSS地址）")
    private String avatar;

    @NotNull(message = "角色不能为空")
    @Schema(description = "角色：0-管理员 1-普通成员 2-关怀成员（老人）")
    private Integer role;

    @Schema(description = "家庭关系（父亲/母亲/爷爷等）")
    private String relation;
}
