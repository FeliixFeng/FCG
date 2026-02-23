package com.ghf.fcg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghf.fcg.common.utils.PasswordEncoder;
import com.ghf.fcg.modules.system.dto.UserRegisterDTO;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.mapper.UserMapper;
import com.ghf.fcg.modules.system.service.IUserService;
import org.apache.ibatis.annotations.Lang;
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
            throw new RuntimeException("用户名已存在");
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
}
