package com.luna.deepluna.service;

import com.luna.deepluna.common.config.AuthProperties;
import com.luna.deepluna.common.security.JwtTokenService;
import com.luna.deepluna.common.utils.AssertUtil;
import com.luna.deepluna.domain.request.LoginRequest;
import com.luna.deepluna.domain.request.RegisterRequest;
import com.luna.deepluna.domain.response.UserResponse;
import com.luna.deepluna.domain.entity.User;
import com.luna.deepluna.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;
    private final JwtTokenService jwtTokenService;
    
    // 用于缓存在线用户的Map，key为userId，value为UserResponse
    private final Map<Long, UserResponse> onlineUsers = new ConcurrentHashMap<>();
    
    /**
     * 用户注册
     */
    public UserResponse register(RegisterRequest request) {
        AssertUtil.isNotEmpty(request.getUserName(), "用户名不能为空");
        AssertUtil.isNotEmpty(request.getPassword(), "密码不能为空");

        // 检查用户名是否已存在
        User existingUser = userRepository.findByUserName(request.getUserName());
        AssertUtil.isNull(existingUser, "用户名已存在");
        
        // 生成用户ID（简单实现，实际应用中可以使用雪花算法等）
        Long userId = System.currentTimeMillis();
        
        // 创建新用户
        User user = User.builder()
                .userId(userId)
                .userName(request.getUserName())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        
        userRepository.save(user);
        
        log.info("用户注册成功: {}", request.getUserName());
        
        return buildUserResponse(userId, request.getUserName());
    }
    
    /**
     * 用户登录
     */
    public UserResponse login(LoginRequest request) {
        AssertUtil.isNotEmpty(request.getUserName(), "用户名不能为空");
        AssertUtil.isNotEmpty(request.getPassword(), "密码不能为空");

        // 验证用户名和密码
        User user = userRepository.findByUserName(request.getUserName());
        AssertUtil.isNotNull(user, "用户不存在");

        String rawPassword = request.getPassword();
        String storedPassword = user.getPassword();
        boolean match = passwordEncoder.matches(rawPassword, storedPassword);

        // 兼容历史明文密码数据：首次登录后自动升级为 BCrypt
        if (!match && Objects.equals(rawPassword, storedPassword)) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
            match = true;
        }
        AssertUtil.isTrue(match, "密码错误");

        UserResponse userResponse = buildUserResponse(user.getUserId(), user.getUserName());
        
        // 添加到在线用户缓存
        onlineUsers.put(user.getUserId(), userResponse);
        
        log.info("用户登录成功: {}", request.getUserName());
        
        return userResponse;
    }
    
    /**
     * 用户退出登录
     */
    public void logout(Long userId) {
        UserResponse user = onlineUsers.remove(userId);
        if (user != null) {
            log.info("用户退出登录: {}", user.getUserName());
        }
    }
    
    /**
     * 检查用户是否在线
     */
    public boolean isOnline(Long userId) {
        return onlineUsers.containsKey(userId);
    }
    
    /**
     * 获取在线用户信息
     */
    public UserResponse getOnlineUser(Long userId) {
        return onlineUsers.get(userId);
    }

    private UserResponse buildUserResponse(Long userId, String userName) {
        String token = null;
        if (authProperties.isJwtMode()) {
            token = jwtTokenService.generateToken(userId, userName);
        }
        return new UserResponse(userId, userName, token);
    }
}
