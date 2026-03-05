package com.luna.deepluna.security;

import com.luna.deepluna.common.config.AuthProperties;
import com.luna.deepluna.common.security.JwtTokenService;
import com.luna.deepluna.domain.entity.User;
import com.luna.deepluna.domain.request.LoginRequest;
import com.luna.deepluna.domain.request.RegisterRequest;
import com.luna.deepluna.domain.response.UserResponse;
import com.luna.deepluna.repository.UserRepository;
import com.luna.deepluna.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthProperties authProperties;

    private UserService userService;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        userService = new UserService(userRepository, passwordEncoder, authProperties, jwtTokenService);
    }

    @Test
    void registerShouldStoreBcryptPassword() {
        RegisterRequest request = new RegisterRequest("alice", "password123");
        when(userRepository.findByUserName("alice")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
    }

    @Test
    void loginShouldAutoUpgradeLegacyPlaintextPassword() {
        User legacyUser = User.builder()
                .userId(1L)
                .userName("alice")
                .password("legacy-pass")
                .build();

        when(userRepository.findByUserName("alice")).thenReturn(legacyUser);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.login(new LoginRequest("alice", "legacy-pass"));

        assertThat(response.getUserId()).isEqualTo(1L);
        verify(userRepository, times(1)).save(any(User.class));
        assertThat(passwordEncoder.matches("legacy-pass", legacyUser.getPassword())).isTrue();
    }

    @Test
    void loginShouldReturnJwtTokenWhenJwtModeEnabled() {
        authProperties.setMode("jwt");
        User user = User.builder()
                .userId(2L)
                .userName("bob")
                .password(passwordEncoder.encode("secure-pass"))
                .build();

        when(userRepository.findByUserName("bob")).thenReturn(user);
        when(jwtTokenService.generateToken(2L, "bob")).thenReturn("jwt-token-value");

        UserResponse response = userService.login(new LoginRequest("bob", "secure-pass"));

        assertThat(response.getToken()).isEqualTo("jwt-token-value");
    }
}
