package com.ghf.fcg.modules.system.controller;

import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.system.dto.UserLoginDTO;
import com.ghf.fcg.modules.system.dto.UserRegisterDTO;
import com.ghf.fcg.modules.system.vo.UserVO;
import com.ghf.fcg.modules.system.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
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
    public Result<UserVO> getUserInfo() {
        // TODO: 获取当前登录用户信息
        return Result.success();
    }

    @PutMapping("/update")
    public Result<Void> updateUserInfo() {
        // TODO: 更新用户信息
        return Result.success();
    }

    @PutMapping("/care-mode/{mode}")
    public Result<Void> switchCareMode(@PathVariable Integer mode) {
        // TODO: 切换关怀模式
        return Result.success();
    }
}
