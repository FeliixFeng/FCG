package com.ghf.fcg.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghf.fcg.modules.system.dto.UserRegisterDTO;
import com.ghf.fcg.modules.system.entity.User;
import jakarta.validation.Valid;

public interface IUserService extends IService<User> {
    // 登陆接口
    Long register(@Valid UserRegisterDTO registerDTO);
}
