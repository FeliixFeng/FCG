package com.ghf.fcg.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghf.fcg.modules.system.dto.MemberUpdateDTO;
import com.ghf.fcg.modules.system.dto.UserUpdateDTO;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.vo.UserVO;

public interface IUserService extends IService<User> {

    /** 获取成员信息 */
    UserVO getUserInfo(Long memberId);

    /** 更新成员信息（昵称/手机号/头像） */
    void updateUserInfo(Long memberId, UserUpdateDTO updateDTO);

    /** 切换关怀模式 */
    void switchCareMode(Long memberId, Integer mode);

    /** 获取成员详情（管理员用） */
    UserVO getMemberDetail(Long memberId);

    /** 更新成员信息（管理员用，包含关系和角色） */
    void updateMember(Long memberId, MemberUpdateDTO updateDTO);

    /** 删除成员 */
    void deleteMember(Long memberId);

    /** 修改成员角色 */
    void updateMemberRole(Long memberId, Integer role);
}
