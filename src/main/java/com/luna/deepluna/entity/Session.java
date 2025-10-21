package com.luna.deepluna.entity;

import com.luna.deepluna.common.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 会话实体类
 */
@Data
@Entity
@Table(name = "session")
public class Session {
    
    @Id
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "model", nullable = false, length = 20)
    private String model;
    
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private SessionStatus status;
    
    @Lob
    @Column(name = "research_brief", columnDefinition = "TEXT")
    private String researchBrief;
    
    @CreationTimestamp
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
    
    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}