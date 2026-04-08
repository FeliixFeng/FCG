package com.ghf.fcg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.modules.system.dto.MemberUpdateDTO;
import com.ghf.fcg.modules.system.dto.UserUpdateDTO;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.entity.UserProfile;
import com.ghf.fcg.modules.system.mapper.UserMapper;
import com.ghf.fcg.modules.system.service.IUserProfileService;
import com.ghf.fcg.modules.system.service.IUserService;
import com.ghf.fcg.modules.system.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final IUserProfileService userProfileService;

    public UserServiceImpl(IUserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * 获取成员信息
     */
    @Override
    public UserVO getUserInfo(Long memberId) {
        User user = this.getById(memberId);
        if (user == null) {
            throw new BusinessException(MessageConstant.USER_NOT_EXIST);
        }
        return UserVO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .familyId(user.getFamilyId())
                .relation(user.getRelation())
                .careMode(user.getCareMode())
                .createTime(user.getCreateTime())
                .build();
    }

    /**
     * 更新成员信息（昵称/手机号/头像）
     */
    @Override
    public void updateUserInfo(Long memberId, UserUpdateDTO updateDTO) {
        User user = this.getById(memberId);
        if (user == null) {
            throw new BusinessException(MessageConstant.USER_NOT_EXIST);
        }
        user.setNickname(updateDTO.getNickname());
        user.setPhone(updateDTO.getPhone());
        user.setAvatar(updateDTO.getAvatar());
        this.updateById(user);
    }

    /**
     * 切换关怀模式
     */
    @Override
    public void switchCareMode(Long memberId, Integer mode) {
        if (mode != User.CARE_MODE_OFF && mode != User.CARE_MODE_ON) {
            throw new BusinessException(MessageConstant.CARE_MODE_PARAM_ERROR);
        }
        User user = this.getById(memberId);
        if (user == null) {
            throw new BusinessException(MessageConstant.USER_NOT_EXIST);
        }
        user.setCareMode(mode);
        this.updateById(user);
    }

    @Override
    public UserVO getMemberDetail(Long memberId) {
        User user = this.getById(memberId);
        if (user == null) {
            throw new BusinessException(MessageConstant.USER_NOT_EXIST);
        }
        var profile = userProfileService.getByUserId(memberId);
        return UserVO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .familyId(user.getFamilyId())
                .relation(user.getRelation())
                .careMode(user.getCareMode())
                .createTime(user.getCreateTime())
                .birthday(profile != null ? profile.getBirthday() : null)
                .height(profile != null ? profile.getHeight() : null)
                .weight(profile != null ? profile.getWeight() : null)
                .disease(profile != null ? profile.getDisease() : null)
                .allergy(profile != null ? profile.getAllergy() : null)
                .remark(profile != null ? profile.getRemark() : null)
                .build();
    }

    @Override
    @Transactional
    public void updateMember(Long memberId, MemberUpdateDTO updateDTO) {
        User user = this.getById(memberId);
        if (user == null) {
            throw new BusinessException(MessageConstant.USER_NOT_EXIST);
        }
        user.setNickname(updateDTO.getNickname());
        if (updateDTO.getPhone() != null) {
            user.setPhone(updateDTO.getPhone());
        }
        if (updateDTO.getAvatar() != null) {
            user.setAvatar(updateDTO.getAvatar());
        }
        if (updateDTO.getRelation() != null) {
            user.setRelation(updateDTO.getRelation());
        }
        if (updateDTO.getRole() != null) {
            if (updateDTO.getRole() < 0 || updateDTO.getRole() > 2) {
                throw new BusinessException("角色值无效");
            }
            user.setRole(updateDTO.getRole());
        }
        this.updateById(user);
        
        // 保存扩展信息
        UserProfile profile = userProfileService.getByUserId(memberId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(memberId);
        }
        if (updateDTO.getBirthday() != null) {
            profile.setBirthday(updateDTO.getBirthday());
        }
        if (updateDTO.getHeight() != null) {
            profile.setHeight(updateDTO.getHeight());
        }
        if (updateDTO.getWeight() != null) {
            profile.setWeight(updateDTO.getWeight());
        }
        if (updateDTO.getDisease() != null) {
            profile.setDisease(updateDTO.getDisease());
        }
        if (updateDTO.getAllergy() != null) {
            profile.setAllergy(updateDTO.getAllergy());
        }
        if (updateDTO.getRemark() != null) {
            profile.setRemark(updateDTO.getRemark());
        }
        userProfileService.saveOrUpdate(memberId, profile);
    }

    @Override
    public void deleteMember(Long memberId) {
        User user = this.getById(memberId);
        if (user == null) {
            throw new BusinessException(MessageConstant.USER_NOT_EXIST);
        }
        this.removeById(memberId);
    }

    @Override
    public void updateMemberRole(Long memberId, Integer role) {
        User user = this.getById(memberId);
        if (user == null) {
            throw new BusinessException(MessageConstant.USER_NOT_EXIST);
        }
        if (role < 0 || role > 2) {
            throw new BusinessException("角色值无效");
        }
        user.setRole(role);
        this.updateById(user);
    }
}
