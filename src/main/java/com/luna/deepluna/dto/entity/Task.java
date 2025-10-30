package com.luna.deepluna.dto.entity;

import com.luna.deepluna.common.enums.AgentTypeEnums;
import com.luna.deepluna.common.enums.SubAgentTaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 任务实体类
 */
@Data
@Entity
@Table(name = "task")
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
    private SubAgentTaskStatus status;
    
    @CreationTimestamp
    @Column(name = "started_time", nullable = false)
    private LocalDateTime startedTime;
    
    @Column(name = "finished_time", nullable = false)
    private LocalDateTime finishedTime;
}