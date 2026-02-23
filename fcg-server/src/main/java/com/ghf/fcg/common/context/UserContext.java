package com.ghf.fcg.common.context;

/**
 * 当前用户上下文（线程级）
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
        private final Long userId;
        private final String username;
        private final Integer role;

        public UserInfo(Long userId, String username, Integer role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public Integer getRole() {
            return role;
        }
    }
}
