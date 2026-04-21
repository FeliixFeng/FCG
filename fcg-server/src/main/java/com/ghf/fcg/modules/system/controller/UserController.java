package com.ghf.fcg.modules.system.controller;

import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.context.UserContext;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.common.result.Result;
import com.ghf.fcg.modules.system.dto.MemberUpdateDTO;
import com.ghf.fcg.modules.system.dto.UserProfileUpdateDTO;
import com.ghf.fcg.modules.system.dto.UserUpdateDTO;
import com.ghf.fcg.modules.system.entity.UserProfile;
import com.ghf.fcg.modules.system.service.IUserProfileService;
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
    private final IUserProfileService userProfileService;

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

    /** 获取成员详情（管理员） */
    @GetMapping("/{userId}")
    @Operation(summary = "获取成员详情")
    public Result<UserVO> getMemberDetail(@PathVariable Long userId) {
        return Result.success(userService.getMemberDetail(userId));
    }

    /** 更新成员信息（管理员） */
    @PutMapping("/{userId}")
    @Operation(summary = "更新成员信息")
    public Result<Void> updateMember(@PathVariable Long userId, @RequestBody MemberUpdateDTO updateDTO) {
        userService.updateMember(userId, updateDTO);
        return Result.success();
    }

    /** 删除成员（管理员） */
    @DeleteMapping("/{userId}")
    @Operation(summary = "删除成员")
    public Result<Void> deleteMember(@PathVariable Long userId) {
        userService.deleteMember(userId);
        return Result.success();
    }

    /** 修改成员角色（管理员） */
    @PutMapping("/{userId}/role")
    @Operation(summary = "修改成员角色")
    public Result<Void> updateMemberRole(@PathVariable Long userId, @RequestParam Integer role) {
        userService.updateMemberRole(userId, role);
        return Result.success();
    }

    /** 获取成员健康档案（身高） */
    @GetMapping("/profile")
    @Operation(summary = "获取成员健康档案")
    public Result<UserProfile> getUserProfile(@RequestParam(required = false) Long userId) {
        Long targetUserId = userId;
        if (targetUserId == null) {
            targetUserId = UserContext.get().getUserId();
        }
        if (targetUserId == null) {
            return Result.success(null);
        }
        UserProfile profile = userProfileService.getByUserId(targetUserId);
        return Result.success(profile);
    }

    /** 更新当前成员健康档案 */
    @PutMapping("/profile")
    @Operation(summary = "更新当前成员健康档案")
    public Result<Void> updateMyProfile(@RequestBody UserProfileUpdateDTO dto) {
        Long memberId = UserContext.get().getMemberId();
        if (memberId == null) {
            throw new BusinessException(MessageConstant.NEED_MEMBER_TOKEN);
        }
        UserProfile profile = userProfileService.getByUserId(memberId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(memberId);
        }
        profile.setBirthday(dto.getBirthday());
        profile.setHeight(dto.getHeight());
        profile.setWeight(dto.getWeight());
        profile.setDisease(dto.getDisease());
        profile.setAllergy(dto.getAllergy());
        profile.setRemark(dto.getRemark());
        userProfileService.saveOrUpdate(memberId, profile);
        return Result.success();
    }
}
