package com.ghf.fcg.common.context;

/**
 * 当前用户上下文（线程级）
 * 从 JWT token 中解析后存入，业务层通过 UserContext.get() 获取当前操作者信息
 */
public final class UserContext {

    private static final ThreadLocal<UserInfo> CONTEXT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserInfo userInfo) {
        CONTEXT.set(userInfo);
    }

    public static UserInfo get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static class UserInfo {
        private final Long familyId;    // 所属家庭ID
        private final Long memberId;    // 当前成员ID（选人后才有）
        private final Integer role;     // 角色：0-管理员 1-普通成员 2-关怀成员

        public UserInfo(Long familyId, Long memberId, Integer role) {
            this.familyId = familyId;
            this.memberId = memberId;
            this.role = role;
        }

        public Long getFamilyId() {
            return familyId;
        }

        /** 兼容旧代码，等同于 getMemberId() */
        public Long getUserId() {
            return memberId;
        }

        public Long getMemberId() {
            return memberId;
        }

        public Integer getRole() {
            return role;
        }
    }
}
