package com.luna.deepluna.domain.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long userId;
    private String userName;
    private String token;

    public UserResponse(Long userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }
}
