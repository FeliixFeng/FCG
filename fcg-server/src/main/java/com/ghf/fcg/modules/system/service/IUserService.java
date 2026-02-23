package com.ghf.fcg.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghf.fcg.modules.system.dto.UserLoginDTO;
import com.ghf.fcg.modules.system.dto.UserRegisterDTO;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.vo.UserVO;
import jakarta.validation.Valid;

public interface IUserService extends IService<User> {
    // 注册接口
    Long register(@Valid UserRegisterDTO registerDTO);

    // 登陆接口
    UserVO login(@Valid UserLoginDTO loginDTO);
}
