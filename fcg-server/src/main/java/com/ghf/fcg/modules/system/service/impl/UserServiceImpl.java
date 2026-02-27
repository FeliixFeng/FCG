package com.ghf.fcg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.modules.system.dto.UserUpdateDTO;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.mapper.UserMapper;
import com.ghf.fcg.modules.system.service.IUserService;
import com.ghf.fcg.modules.system.vo.UserVO;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

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
}
