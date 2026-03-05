package com.luna.deepluna.security;

import com.luna.deepluna.common.config.SecurityConfig;
import com.luna.deepluna.common.security.JwtAuthenticationFilter;
import com.luna.deepluna.common.security.JwtTokenService;
import com.luna.deepluna.controller.SessionController;
import com.luna.deepluna.domain.response.SessionResponse;
import com.luna.deepluna.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SessionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenService.class})
@TestPropertySource(properties = {
        "auth.mode=jwt",
        "auth.jwt.secret=test-jwt-secret-test-jwt-secret-1234567890"
})
class AuthModeJwtSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private SessionService sessionService;

    // DeepLunaApplication 启用了 @EnableJpaAuditing，WebMvcTest 需显式 mock 掉 JPA metamodel
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void shouldRejectAnonymousRequestWhenJwtModeEnabled() throws Exception {
        mockMvc.perform(get("/capi/session/list/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowRequestWithValidJwtWhenJwtModeEnabled() throws Exception {
        when(sessionService.getUserSessions(anyLong()))
                .thenReturn(List.of(new SessionResponse("session-1", "summary")));

        String token = jwtTokenService.generateToken(1L, "tester");
        mockMvc.perform(get("/capi/session/list/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
