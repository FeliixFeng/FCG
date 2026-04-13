package com.ghf.fcg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghf.fcg.modules.system.entity.UserProfile;
import com.ghf.fcg.modules.system.mapper.UserProfileMapper;
import com.ghf.fcg.modules.system.service.IUserProfileService;
import org.springframework.stereotype.Service;

@Service
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile> implements IUserProfileService {

    @Override
    public UserProfile getByUserId(Long userId) {
        return this.getOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId));
    }

    @Override
    public void saveOrUpdate(Long userId, UserProfile profile) {
        UserProfile existing = getByUserId(userId);
        if (existing != null) {
            profile.setId(existing.getId());
            this.updateById(profile);
        } else {
            profile.setUserId(userId);
            this.save(profile);
        }
    }
}