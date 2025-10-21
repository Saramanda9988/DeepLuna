package com.luna.deepluna.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 用户实体类
 */
@Data
@Entity
@Table(name = "user")
public class User {
    
    @Id
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "user_name", nullable = false)
    private String userName;
    
    @Column(name = "password", nullable = false)
    private String password;
}