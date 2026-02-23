package com.ghf.fcg.modules.system.controller;

import com.ghf.fcg.common.context.UserContext;
import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.system.dto.UserLoginDTO;
import com.ghf.fcg.modules.system.dto.UserRegisterDTO;
import com.ghf.fcg.modules.system.dto.UserUpdateDTO;
import com.ghf.fcg.modules.system.vo.UserVO;
import com.ghf.fcg.modules.system.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户模块", description = "用户注册、登录与个人信息")
public class UserController {

    private final IUserService userService;

    @Operation(summary = "用户登陆")
    @PostMapping("/login")
    public Result<UserVO> login(@RequestBody @Valid UserLoginDTO loginDTO) {
        UserVO userVO = userService.login(loginDTO);
        return Result.success(userVO);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Long> register(@RequestBody @Valid UserRegisterDTO registerDTO) {
        Long userId = userService.register(registerDTO);
        return Result.success(userId);
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserVO> getUserInfo() {
        Long userId = UserContext.get().getUserId();
        UserVO userVO = userService.getUserInfo(userId);
        return Result.success(userVO);
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户信息")
    public Result<Void> updateUserInfo(@RequestBody @Valid UserUpdateDTO updateDTO) {
        Long userId = UserContext.get().getUserId();
        userService.updateUserInfo(userId, updateDTO);
        return Result.success();
    }

    @PutMapping("/care-mode/{mode}")
    @Operation(summary = "切换关怀模式")
    public Result<Void> switchCareMode(@PathVariable Integer mode) {
        Long userId = UserContext.get().getUserId();
        userService.switchCareMode(userId, mode);
        return Result.success();
    }
}
