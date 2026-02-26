package com.ghf.fcg.modules.system.controller;

import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.context.UserContext;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.system.dto.FamilyLoginDTO;
import com.ghf.fcg.modules.system.dto.FamilyRegisterDTO;
import com.ghf.fcg.modules.system.dto.MemberCreateDTO;
import com.ghf.fcg.modules.system.service.IFamilyService;
import com.ghf.fcg.modules.system.vo.FamilyVO;
import com.ghf.fcg.modules.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family")
@RequiredArgsConstructor
@Tag(name = "家庭模块", description = "家庭注册、登录、选人、成员管理")
public class FamilyController {

    private final IFamilyService familyService;

    /** 家庭注册（公开接口，无需 token） */
    @PostMapping("/register")
    @Operation(summary = "家庭注册")
    public Result<FamilyVO> register(@RequestBody @Valid FamilyRegisterDTO dto) {
        return Result.success(familyService.register(dto));
    }

    /** 家庭账号登录，返回家庭级 token（公开接口，无需 token） */
    @PostMapping("/login")
    @Operation(summary = "家庭账号登录")
    public Result<FamilyVO> login(@RequestBody @Valid FamilyLoginDTO dto) {
        return Result.success(familyService.login(dto));
    }

    /** 获取家庭信息（需要家庭级或成员级 token） */
    @GetMapping("/info")
    @Operation(summary = "获取家庭信息")
    public Result<FamilyVO> getFamilyInfo() {
        Long familyId = UserContext.get().getFamilyId();
        return Result.success(familyService.getFamilyInfo(familyId));
    }

    /** 获取家庭成员列表（选人页用，需要家庭级 token） */
    @GetMapping("/members")
    @Operation(summary = "获取家庭成员列表")
    public Result<List<FamilyVO.MemberInfo>> getMembers() {
        Long familyId = UserContext.get().getFamilyId();
        return Result.success(familyService.getMembers(familyId));
    }

    /** 选择成员，返回成员级 token */
    @PostMapping("/switch-member/{memberId}")
    @Operation(summary = "选择成员（选人页点击后调用）")
    public Result<UserVO> switchMember(@PathVariable Long memberId) {
        Long familyId = UserContext.get().getFamilyId();
        return Result.success(familyService.switchMember(familyId, memberId));
    }

    /** 验证管理员密码（进入管理员界面前调用，需要成员级 token） */
    @PostMapping("/verify-admin")
    @Operation(summary = "验证管理员密码")
    public Result<Void> verifyAdmin(@RequestParam String password) {
        Long familyId = UserContext.get().getFamilyId();
        familyService.verifyAdmin(familyId, password);
        return Result.success();
    }

    /** 添加家庭成员（仅管理员可调用，需要成员级 token） */
    @PostMapping("/add-member")
    @Operation(summary = "添加家庭成员（仅管理员）")
    public Result<UserVO> addMember(@RequestBody @Valid MemberCreateDTO dto) {
        UserContext.UserInfo ctx = UserContext.get();
        // 校验是否为管理员
        if (ctx.getRole() == null || ctx.getRole() != 0) {
            throw new BusinessException(MessageConstant.NO_PERMISSION);
        }
        return Result.success(familyService.addMember(ctx.getFamilyId(), dto));
    }
}
