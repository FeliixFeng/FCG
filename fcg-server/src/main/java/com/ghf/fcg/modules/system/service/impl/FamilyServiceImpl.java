package com.ghf.fcg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.modules.system.entity.Family;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.mapper.FamilyMapper;
import com.ghf.fcg.modules.system.service.IFamilyService;
import com.ghf.fcg.modules.system.service.IUserService;
import com.ghf.fcg.modules.system.vo.FamilyVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FamilyServiceImpl extends ServiceImpl<FamilyMapper, Family> implements IFamilyService {

    private final IUserService userService;

    public FamilyServiceImpl(IUserService userService) {
        this.userService = userService;
    }

    @Override
    public FamilyVO createFamily(Long userId, String familyName) {
        User user = userService.getById(userId);
        if (user.getFamilyId() != null) {
            throw new BusinessException("用户已加入家庭，无法创建");
        }

        Family family = new Family();
        family.setFamilyName(familyName);
        family.setInviteCode(generateInviteCode());
        family.setCreatorId(userId);
        this.save(family);

        user.setFamilyId(family.getId());
        user.setRelation("户主");
        userService.updateById(user);

        return FamilyVO.builder()
                .id(family.getId())
                .familyName(family.getFamilyName())
                .inviteCode(family.getInviteCode())
                .createTime(family.getCreateTime())
                .build();
    }

    @Override
    public FamilyVO joinFamily(Long userId, String inviteCode) {
        User user = userService.getById(userId);
        if (user.getFamilyId() != null) {
            throw new BusinessException("用户已加入家庭");
        }

        LambdaQueryWrapper<Family> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Family::getInviteCode, inviteCode);
        Family family = this.getOne(wrapper);
        if (family == null) {
            throw new BusinessException("邀请码无效");
        }

        user.setFamilyId(family.getId());
        user.setRelation("成员");
        userService.updateById(user);

        return FamilyVO.builder()
                .id(family.getId())
                .familyName(family.getFamilyName())
                .inviteCode(family.getInviteCode())
                .createTime(family.getCreateTime())
                .build();
    }

    @Override
    public FamilyVO getFamilyInfo(Long userId) {
        User user = userService.getById(userId);
        if (user.getFamilyId() == null) {
            throw new BusinessException("用户未加入家庭");
        }

        Family family = this.getById(user.getFamilyId());
        if (family == null) {
            throw new BusinessException("家庭不存在");
        }

        return FamilyVO.builder()
                .id(family.getId())
                .familyName(family.getFamilyName())
                .inviteCode(family.getInviteCode())
                .createTime(family.getCreateTime())
                .build();
    }

    @Override
    public List<FamilyVO.MemberInfo> getFamilyMembers(Long userId) {
        User user = userService.getById(userId);
        if (user.getFamilyId() == null) {
            throw new BusinessException("用户未加入家庭");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getFamilyId, user.getFamilyId());
        List<User> members = userService.list(wrapper);

        List<FamilyVO.MemberInfo> result = new ArrayList<>();
        for (User member : members) {
            result.add(FamilyVO.MemberInfo.builder()
                    .userId(member.getId())
                    .username(member.getUsername())
                    .nickname(member.getNickname())
                    .role(member.getRole())
                    .relation(member.getRelation())
                    .careMode(member.getCareMode())
                    .joinTime(member.getCreateTime())
                    .build());
        }
        return result;
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
