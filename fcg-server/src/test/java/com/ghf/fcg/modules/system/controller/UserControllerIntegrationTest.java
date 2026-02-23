package com.ghf.fcg.modules.system.controller;

import com.ghf.fcg.common.interceptor.JwtAuthInterceptor;
import com.ghf.fcg.common.utils.JwtUtils;
import com.ghf.fcg.config.WebMvcConfig;
import com.ghf.fcg.modules.system.dto.UserLoginDTO;
import com.ghf.fcg.modules.system.service.IUserService;
import com.ghf.fcg.modules.system.vo.UserVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({WebMvcConfig.class, JwtAuthInterceptor.class, JwtUtils.class})
@TestPropertySource(properties = {
        "jwt.secret=fcg-jwt-test-secret-key-32-bytes-min-123",
        "jwt.expiration=86400000"
})
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IUserService userService;

    @Test
    void login_then_get_user_info_should_succeed() throws Exception {
        UserVO loginVO = UserVO.builder()
                .id(2L)
                .username("test01")
                .nickname("测试用户")
                .role(1)
                .token("mock-token")
                .build();
        Mockito.when(userService.login(any(UserLoginDTO.class))).thenReturn(loginVO);

        UserVO infoVO = UserVO.builder()
                .id(2L)
                .username("test01")
                .nickname("测试用户")
                .role(1)
                .build();
        Mockito.when(userService.getUserInfo(eq(2L))).thenReturn(infoVO);

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("test01");
        loginDTO.setPassword("123456");

        mockMvc.perform(post("/api/user/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("test01"));

        String token = JwtUtils.generateToken(2L, "test01", 1);

        mockMvc.perform(get("/api/user/info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2));
    }
}
