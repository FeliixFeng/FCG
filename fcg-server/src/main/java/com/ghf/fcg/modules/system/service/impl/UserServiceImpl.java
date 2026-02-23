package com.ghf.fcg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghf.fcg.common.exception.BusinessException;
import com.ghf.fcg.common.utils.JwtUtils;
import com.ghf.fcg.common.utils.PasswordEncoder;
import com.ghf.fcg.modules.system.dto.UserLoginDTO;
import com.ghf.fcg.modules.system.dto.UserRegisterDTO;
import com.ghf.fcg.modules.system.dto.UserUpdateDTO;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.mapper.UserMapper;
import com.ghf.fcg.modules.system.service.IUserService;
import com.ghf.fcg.modules.system.vo.UserVO;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Override
    public Long register(UserRegisterDTO registerDTO) {
        // 检查用户名是否存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, registerDTO.getUsername());
        Long count = this.count(wrapper);
        if(count > 0){
            throw new BusinessException("用户名已存在");
        }

        // 加密密码
        String encodedPassword = PasswordEncoder.encode(registerDTO.getPassword());

        // 创建用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(encodedPassword);
        user.setNickname(registerDTO.getNickname());
        user.setPhone(registerDTO.getPhone());
        user.setRole(User.ROLE_MEMBER);
        user.setCareMode(User.CARE_MODE_OFF);

        // 保存
        this.save(user);
        return user.getId();
    }

    @Override
    public UserVO login(UserLoginDTO loginDTO) {
        // 1. 根据用户名查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = this.getOne(wrapper);
        if(user == null){
            throw new BusinessException("用户名或密码错误");
        }

        // 2. 验证密码
        if(!PasswordEncoder.matches(loginDTO.getPassword(), user.getPassword())){
            throw new BusinessException("用户名或密码错误");
        }
        // 3. 生成token
        String token = JwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 4. 返回userVO
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .token(token)
                .build();
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .familyId(user.getFamilyId())
                .careMode(user.getCareMode())
                .createTime(user.getCreateTime())
                .build();
    }

    @Override
    public void updateUserInfo(Long userId, UserUpdateDTO updateDTO) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setNickname(updateDTO.getNickname());
        user.setPhone(updateDTO.getPhone());
        user.setAvatar(updateDTO.getAvatar());
        this.updateById(user);
    }

    @Override
    public void switchCareMode(Long userId, Integer mode) {
        if (mode != User.CARE_MODE_OFF && mode != User.CARE_MODE_ON) {
            throw new BusinessException("关怀模式参数错误");
        }
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setCareMode(mode);
        this.updateById(user);
    }
}
