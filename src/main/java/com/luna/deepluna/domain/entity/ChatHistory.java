package com.luna.deepluna.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 澄清历史实体类
 */
@Data
@Entity
@Table(name = "chat_history")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistory {
    
    @Id
    @Column(name = "id")
    private String id;
    
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @Column(name = "question")
    private String question;
    
    @Lob
    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;
    
    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;
    
    @Column(name = "completed", nullable = false)
    private Boolean completed;
    
    @CreationTimestamp
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
}