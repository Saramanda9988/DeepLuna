package com.luna.deepluna.entity;

import com.luna.deepluna.common.enums.AgentTypeEnums;
import com.luna.deepluna.common.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 任务实体类
 */
@Data
@Entity
@Table(name = "task")
public class Task {
    
    @Id
    @Column(name = "id")
    private String id;
    
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "agent_type", nullable = false)
    private AgentTypeEnums agentType;

//    @JdbcTypeCode(SqlTypes.JSON)
    @Lob
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;
    
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private TaskStatus status;
    
    @CreationTimestamp
    @Column(name = "started_time", nullable = false)
    private LocalDateTime startedTime;
    
    @Column(name = "finished_time", nullable = false)
    private LocalDateTime finishedTime;
}