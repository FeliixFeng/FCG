package com.ghf.fcg.modules.system.controller;

import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.system.dto.UserLoginDTO;
import com.ghf.fcg.modules.system.dto.UserRegisterDTO;
import com.ghf.fcg.modules.system.vo.UserVO;
import com.ghf.fcg.modules.system.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @PostMapping("/login")
    public Result<UserVO> login(@RequestBody @Valid UserLoginDTO loginDTO) {
        // TODO: 调用 Service 登录逻辑

        return Result.ok();
    }

    @PostMapping("/register")
    public Result<Long> register(@RequestBody @Valid UserRegisterDTO registerDTO) {
        // TODO: 调用 Service 注册逻辑
        userService.register(registerDTO);
        return Result.ok();
    }

    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        // TODO: 获取当前登录用户信息
        return Result.ok();
    }

    @PutMapping("/update")
    public Result<Void> updateUserInfo() {
        // TODO: 更新用户信息
        return Result.ok();
    }

    @PutMapping("/care-mode/{mode}")
    public Result<Void> switchCareMode(@PathVariable Integer mode) {
        // TODO: 切换关怀模式
        return Result.ok();
    }
}
