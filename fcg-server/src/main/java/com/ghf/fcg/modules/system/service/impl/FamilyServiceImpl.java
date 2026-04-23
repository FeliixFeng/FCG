package com.ghf.fcg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghf.fcg.common.constant.MessageConstant;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.common.utils.JwtUtils;
import com.ghf.fcg.common.utils.PasswordEncoder;
import com.ghf.fcg.modules.system.dto.FamilyLoginDTO;
import com.ghf.fcg.modules.system.dto.FamilyRegisterDTO;
import com.ghf.fcg.modules.system.dto.MemberCreateDTO;
import com.ghf.fcg.modules.system.entity.Family;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.mapper.FamilyMapper;
import com.ghf.fcg.modules.system.service.IFamilyService;
import com.ghf.fcg.modules.system.service.IUserProfileService;
import com.ghf.fcg.modules.system.service.IUserService;
import com.ghf.fcg.modules.system.vo.FamilyVO;
import com.ghf.fcg.modules.system.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FamilyServiceImpl extends ServiceImpl<FamilyMapper, Family> implements IFamilyService {

    private final IUserService userService;
    private final IUserProfileService userProfileService;

    public FamilyServiceImpl(IUserService userService, IUserProfileService userProfileService) {
        this.userService = userService;
        this.userProfileService = userProfileService;
    }

    /**
     * 家庭注册
     * 1. 检查账号唯一性
     * 2. 创建家庭记录
     * 3. 创建第一个成员（管理员），并将 creatorId 回填到家庭表
     */
    @Override
    @Transactional
    public FamilyVO register(FamilyRegisterDTO dto) {
        // 检查账号是否已存在
        Long count = this.count(new LambdaQueryWrapper<Family>()
                .eq(Family::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(MessageConstant.FAMILY_USERNAME_EXIST);
        }

        // 家庭名称默认值
        String familyName = (dto.getFamilyName() != null && !dto.getFamilyName().isBlank())
                ? dto.getFamilyName()
                : dto.getUsername() + "的家";

        // 创建家庭（creatorId 先占位，后面回填）
        Family family = new Family();
        family.setFamilyName(familyName);
        family.setUsername(dto.getUsername());
        family.setPassword(PasswordEncoder.encode(dto.getPassword()));
        family.setCreatorId(0L);
        family.setLowStockThreshold(5);
        family.setExpiringDays(30);
        this.save(family);

        // 创建第一个成员（管理员）
        User admin = new User();
        admin.setNickname(dto.getAdminNickname());
        admin.setRole(User.ROLE_ADMIN);
        admin.setFamilyId(family.getId());
        admin.setRelation("户主");
        admin.setCareMode(User.CARE_MODE_OFF);
        userService.save(admin);

        // 回填 creatorId，重新查询拿到完整数据（含 createTime）
        family.setCreatorId(admin.getId());
        this.updateById(family);
        family = this.getById(family.getId());

        return FamilyVO.builder()
                .id(family.getId())
                .familyName(family.getFamilyName())
                .username(family.getUsername())
                .lowStockThreshold(family.getLowStockThreshold())
                .expiringDays(family.getExpiringDays())
                .createTime(family.getCreateTime())
                .build();
    }

    /**
     * 家庭账号登录
     * 验证账号密码，返回家庭级 token（只含 familyId，用于选人页）
     */
    @Override
    public FamilyVO login(FamilyLoginDTO dto) {
        Family family = this.getOne(new LambdaQueryWrapper<Family>()
                .eq(Family::getUsername, dto.getUsername()));
        if (family == null || !PasswordEncoder.matches(dto.getPassword(), family.getPassword())) {
            throw new BusinessException(MessageConstant.LOGIN_FAILED);
        }

        // 生成家庭级 token
        String token = JwtUtils.generateFamilyToken(family.getId(), family.getUsername());

        return FamilyVO.builder()
                .id(family.getId())
                .familyName(family.getFamilyName())
                .username(family.getUsername())
                .token(token)
                .lowStockThreshold(family.getLowStockThreshold())
                .expiringDays(family.getExpiringDays())
                .createTime(family.getCreateTime())
                .build();
    }

    /**
     * 获取家庭成员列表（选人页展示用）
     */
    @Override
    public List<FamilyVO.MemberInfo> getMembers(Long familyId) {
        List<User> members = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getFamilyId, familyId));

        return members.stream().map(m -> {
            var profile = userProfileService.getByUserId(m.getId());
            return FamilyVO.MemberInfo.builder()
                    .userId(m.getId())
                    .nickname(m.getNickname())
                    .avatar(m.getAvatar())
                    .role(m.getRole())
                    .relation(m.getRelation())
                    .phone(m.getPhone())
                    .careMode(m.getCareMode())
                    .joinTime(m.getCreateTime())
                    .birthday(profile != null ? profile.getBirthday() : null)
                    .height(profile != null ? profile.getHeight() : null)
                    .weight(profile != null ? profile.getWeight() : null)
                    .disease(profile != null ? profile.getDisease() : null)
                    .allergy(profile != null ? profile.getAllergy() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 选择成员，返回成员级 token（含 familyId + memberId + role）
     */
    @Override
    public UserVO switchMember(Long familyId, Long memberId) {
        User member = userService.getById(memberId);
        if (member == null || !familyId.equals(member.getFamilyId())) {
            throw new BusinessException(MessageConstant.USER_NOT_IN_FAMILY);
        }

        // 生成成员级 token
        String token = JwtUtils.generateMemberToken(familyId, memberId, member.getRole());

        return UserVO.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .avatar(member.getAvatar())
                .role(member.getRole())
                .familyId(familyId)
                .relation(member.getRelation())
                .careMode(member.getCareMode())
                .token(token)
                .createTime(member.getCreateTime())
                .build();
    }

    /**
     * 验证管理员密码（进入管理员界面前调用）
     * 用家庭账号密码验证
     */
    @Override
    public void verifyAdmin(Long familyId, String password) {
        Family family = this.getById(familyId);
        if (family == null || !PasswordEncoder.matches(password, family.getPassword())) {
            throw new BusinessException(MessageConstant.ADMIN_VERIFY_FAILED);
        }
    }

    /**
     * 添加家庭成员（仅管理员可调用）
     */
    @Override
    public UserVO addMember(Long familyId, MemberCreateDTO dto) {
        User member = new User();
        member.setNickname(dto.getNickname());
        member.setPhone(dto.getPhone());
        member.setAvatar(dto.getAvatar());
        member.setRole(dto.getRole());
        member.setFamilyId(familyId);
        member.setRelation(dto.getRelation());
        // 关怀成员自动开启关怀模式
        member.setCareMode(dto.getRole() == User.ROLE_CARE ? User.CARE_MODE_ON : User.CARE_MODE_OFF);
        userService.save(member);

        return UserVO.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .avatar(member.getAvatar())
                .role(member.getRole())
                .familyId(familyId)
                .relation(member.getRelation())
                .careMode(member.getCareMode())
                .createTime(member.getCreateTime())
                .build();
    }

    /**
     * 获取家庭信息
     */
    @Override
    public FamilyVO getFamilyInfo(Long familyId) {
        Family family = this.getById(familyId);
        if (family == null) {
            throw new BusinessException(MessageConstant.FAMILY_NOT_EXIST);
        }
        return FamilyVO.builder()
                .id(family.getId())
                .familyName(family.getFamilyName())
                .username(family.getUsername())
                .lowStockThreshold(family.getLowStockThreshold())
                .expiringDays(family.getExpiringDays())
                .createTime(family.getCreateTime())
                .build();
    }

    @Override
    public FamilyVO updateFamilySettings(Long familyId, String familyName, Integer lowStockThreshold, Integer expiringDays) {
        Family family = this.getById(familyId);
        if (family == null) {
            throw new BusinessException(MessageConstant.FAMILY_NOT_EXIST);
        }

        String normalized = familyName == null ? "" : familyName.trim();
        if (normalized.isEmpty() || normalized.length() > 30 || lowStockThreshold == null || expiringDays == null) {
            throw new BusinessException(MessageConstant.PARAM_ERROR);
        }
        if (lowStockThreshold < 1 || lowStockThreshold > 999 || expiringDays < 1 || expiringDays > 365) {
            throw new BusinessException(MessageConstant.PARAM_ERROR);
        }

        family.setFamilyName(normalized);
        family.setLowStockThreshold(lowStockThreshold);
        family.setExpiringDays(expiringDays);
        this.updateById(family);
        return getFamilyInfo(familyId);
    }

    @Override
    public void updateFamilyPassword(Long familyId, String oldPassword, String newPassword) {
        Family family = this.getById(familyId);
        if (family == null) {
            throw new BusinessException(MessageConstant.FAMILY_NOT_EXIST);
        }
        if (!PasswordEncoder.matches(oldPassword, family.getPassword())) {
            throw new BusinessException(MessageConstant.ADMIN_VERIFY_FAILED);
        }
        family.setPassword(PasswordEncoder.encode(newPassword));
        this.updateById(family);
    }
}
