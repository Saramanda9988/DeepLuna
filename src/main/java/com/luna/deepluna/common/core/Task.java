package com.luna.deepluna.common.core;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a task to be executed by an agent.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Task {
    
    private UUID id;
    private String type;
    private Object data;
    private UUID sessionId;
    private LocalDateTime createdAt;
    private Map<String, Object> parameters;
    
    public static Task create(String type, Object data, UUID sessionId) {
        return Task.builder()
                .id(UUID.randomUUID())
                .type(type)
                .data(data)
                .sessionId(sessionId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}