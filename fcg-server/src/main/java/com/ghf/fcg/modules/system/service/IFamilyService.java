package com.ghf.fcg.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghf.fcg.modules.system.entity.Family;
import com.ghf.fcg.modules.system.vo.FamilyVO;

import java.util.List;

public interface IFamilyService extends IService<Family> {
    FamilyVO createFamily(Long userId, String familyName);

    FamilyVO joinFamily(Long userId, String inviteCode);

    FamilyVO getFamilyInfo(Long userId);

    List<FamilyVO.MemberInfo> getFamilyMembers(Long userId);
}
