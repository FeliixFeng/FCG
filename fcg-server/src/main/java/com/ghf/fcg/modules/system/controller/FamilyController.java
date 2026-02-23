package com.ghf.fcg.modules.system.controller;

import com.ghf.fcg.common.context.UserContext;
import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.system.service.IFamilyService;
import com.ghf.fcg.modules.system.vo.FamilyVO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family")
@RequiredArgsConstructor
public class FamilyController {

    private final IFamilyService familyService;

    @PostMapping("/create")
    @Operation(summary = "创建家庭")
    public Result<FamilyVO> createFamily(@RequestParam String familyName) {
        Long userId = UserContext.get().getUserId();
        FamilyVO familyVO = familyService.createFamily(userId, familyName);
        return Result.success(familyVO);
    }

    @PostMapping("/join")
    @Operation(summary = "加入家庭")
    public Result<FamilyVO> joinFamily(@RequestParam String inviteCode) {
        Long userId = UserContext.get().getUserId();
        FamilyVO familyVO = familyService.joinFamily(userId, inviteCode);
        return Result.success(familyVO);
    }

    @GetMapping("/info")
    @Operation(summary = "获取家庭信息")
    public Result<FamilyVO> getFamilyInfo() {
        Long userId = UserContext.get().getUserId();
        FamilyVO familyVO = familyService.getFamilyInfo(userId);
        return Result.success(familyVO);
    }

    @GetMapping("/members")
    @Operation(summary = "获取家庭成员列表")
    public Result<List<FamilyVO.MemberInfo>> getFamilyMembers() {
        Long userId = UserContext.get().getUserId();
        List<FamilyVO.MemberInfo> members = familyService.getFamilyMembers(userId);
        return Result.success(members);
    }
}
