package com.luna.deepluna.service;

import com.luna.deepluna.common.utils.AssertUtil;
import com.luna.deepluna.dto.request.LoginRequest;
import com.luna.deepluna.dto.request.RegisterRequest;
import com.luna.deepluna.dto.response.UserResponse;
import com.luna.deepluna.dto.entity.User;
import com.luna.deepluna.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    // 用于缓存在线用户的Map，key为userId，value为UserResponse
    private final Map<Long, UserResponse> onlineUsers = new ConcurrentHashMap<>();
    
    /**
     * 用户注册
     */
    public UserResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        User existingUser = userRepository.findByUserName(request.getUserName());
        AssertUtil.isNotNull(existingUser, "用户名已存在");
        
        // 生成用户ID（简单实现，实际应用中可以使用雪花算法等）
        Long userId = System.currentTimeMillis();
        
        // 创建新用户
        User user = User.builder()
                .userId(userId)
                .userName(request.getUserName())
                .password(request.getPassword()) // 实际应用中应该加密密码
                .build();
        
        userRepository.save(user);
        
        log.info("用户注册成功: {}", request.getUserName());
        
        return new UserResponse(userId, request.getUserName());
    }
    
    /**
     * 用户登录
     */
    public UserResponse login(LoginRequest request) {
        // 验证用户名和密码
        User user = userRepository.findByUserName(request.getUserName());
        AssertUtil.isNotNull(user, "用户不存在");
        
        AssertUtil.notEqual(user.getPassword(), request.getPassword(), "密码错误");
        
        UserResponse userResponse = new UserResponse(user.getUserId(), user.getUserName());
        
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
}
