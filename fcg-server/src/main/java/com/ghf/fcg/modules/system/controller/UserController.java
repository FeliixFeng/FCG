package com.ghf.fcg.modules.system.controller;

import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.context.UserContext;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.system.dto.UserUpdateDTO;
import com.ghf.fcg.modules.system.service.IUserService;
import com.ghf.fcg.modules.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "成员模块", description = "成员信息查询与更新")
public class UserController {

    private final IUserService userService;

    /** 获取当前成员信息（需要成员级 token） */
    @GetMapping("/info")
    @Operation(summary = "获取当前成员信息")
    public Result<UserVO> getUserInfo() {
        Long memberId = UserContext.get().getMemberId();
        if (memberId == null) {
            throw new BusinessException(MessageConstant.NEED_MEMBER_TOKEN);
        }
        return Result.success(userService.getUserInfo(memberId));
    }

    /** 更新成员信息（昵称/手机号/头像） */
    @PutMapping("/update")
    @Operation(summary = "更新成员信息")
    public Result<Void> updateUserInfo(@RequestBody @Valid UserUpdateDTO updateDTO) {
        Long memberId = UserContext.get().getMemberId();
        if (memberId == null) {
            throw new BusinessException(MessageConstant.NEED_MEMBER_TOKEN);
        }
        userService.updateUserInfo(memberId, updateDTO);
        return Result.success();
    }

    /** 切换关怀模式（0-关闭 1-开启） */
    @PutMapping("/care-mode/{mode}")
    @Operation(summary = "切换关怀模式")
    public Result<Void> switchCareMode(@PathVariable Integer mode) {
        Long memberId = UserContext.get().getMemberId();
        if (memberId == null) {
            throw new BusinessException(MessageConstant.NEED_MEMBER_TOKEN);
        }
        userService.switchCareMode(memberId, mode);
        return Result.success();
    }
}
