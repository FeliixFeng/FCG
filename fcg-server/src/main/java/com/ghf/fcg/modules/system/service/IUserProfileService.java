package com.ghf.fcg.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghf.fcg.modules.system.entity.UserProfile;

public interface IUserProfileService extends IService<UserProfile> {

    UserProfile getByUserId(Long userId);

    void saveOrUpdate(Long userId, UserProfile profile);
}