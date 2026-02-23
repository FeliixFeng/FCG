package com.ghf.fcg.common.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码加密工具类
 */
@Component
public class PasswordEncoder {

    private static final BCryptPasswordEncoder BCRYPT_PASSWORD_ENCODER = new BCryptPasswordEncoder();

    public String encode(String rawPassword) {
        return BCRYPT_PASSWORD_ENCODER.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return BCRYPT_PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
    }
}
