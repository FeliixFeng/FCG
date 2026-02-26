package com.ghf.fcg.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghf.fcg.modules.system.dto.FamilyLoginDTO;
import com.ghf.fcg.modules.system.dto.FamilyRegisterDTO;
import com.ghf.fcg.modules.system.dto.MemberCreateDTO;
import com.ghf.fcg.modules.system.entity.Family;
import com.ghf.fcg.modules.system.vo.FamilyVO;
import com.ghf.fcg.modules.system.vo.UserVO;

import java.util.List;

public interface IFamilyService extends IService<Family> {

    /** 家庭注册（创建家庭账号 + 第一个管理员成员） */
    FamilyVO register(FamilyRegisterDTO dto);

    /** 家庭账号登录，返回家庭级 token */
    FamilyVO login(FamilyLoginDTO dto);

    /** 获取家庭成员列表（选人页用） */
    List<FamilyVO.MemberInfo> getMembers(Long familyId);

    /** 选择成员，返回成员级 token */
    UserVO switchMember(Long familyId, Long memberId);

    /** 验证管理员密码（进入管理员界面前调用） */
    void verifyAdmin(Long familyId, String password);

    /** 添加家庭成员（仅管理员可调用） */
    UserVO addMember(Long familyId, MemberCreateDTO dto);

    /** 获取家庭信息 */
    FamilyVO getFamilyInfo(Long familyId);
}
