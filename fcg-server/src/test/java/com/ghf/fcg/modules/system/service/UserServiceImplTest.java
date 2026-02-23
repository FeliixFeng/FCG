package com.ghf.fcg.modules.system.service;

import com.ghf.fcg.common.utils.JwtUtils;
import com.ghf.fcg.common.utils.PasswordEncoder;
import com.ghf.fcg.modules.system.dto.UserLoginDTO;
import com.ghf.fcg.modules.system.dto.UserRegisterDTO;
import com.ghf.fcg.modules.system.entity.User;
import com.ghf.fcg.modules.system.mapper.UserMapper;
import com.ghf.fcg.modules.system.service.impl.UserServiceImpl;
import com.ghf.fcg.modules.system.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String JWT_SECRET = "fcg-jwt-test-secret-key-32-bytes-min-123";

    @Mock
    private UserMapper userMapper;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl();
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
        ReflectionTestUtils.setField(JwtUtils.class, "secret", JWT_SECRET);
        ReflectionTestUtils.setField(JwtUtils.class, "expiration", 86400000L);
    }

    @Test
    void register_shouldCreateUserAndReturnId() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("test_user");
        dto.setPassword("password123");
        dto.setNickname("Test");
        dto.setPhone("13800138000");

        when(userMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        Long userId = userService.register(dto);
        assertEquals(1L, userId);
    }

    @Test
    void login_shouldReturnToken() {
        User user = new User();
        user.setId(1L);
        user.setUsername("test_user");
        user.setPassword(PasswordEncoder.encode("password123"));
        user.setRole(User.ROLE_MEMBER);

        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(user);

        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("test_user");
        dto.setPassword("password123");

        UserVO userVO = userService.login(dto);
        assertNotNull(userVO);
        assertNotNull(userVO.getToken());
    }
}
