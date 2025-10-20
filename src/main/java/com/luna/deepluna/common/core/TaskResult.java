package com.luna.deepluna.common.core;

import com.luna.deepluna.common.enums.TaskStatus;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents the result of a task execution by an agent.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskResult {
    
    private String taskId;

    private TaskStatus status;

    private Object data;

    private String errorMessage;

    private LocalDateTime completedAt;
    
    public static TaskResult success(String taskId, Object data) {
        return TaskResult.builder()
                .taskId(taskId)
                .status(TaskStatus.COMPLETED)
                .data(data)
                .completedAt(LocalDateTime.now())
                .build();
    }
    
    public static TaskResult failure(String taskId, String errorMessage) {
        return TaskResult.builder()
                .taskId(taskId)
                .status(TaskStatus.FAILED)
                .errorMessage(errorMessage)
                .completedAt(LocalDateTime.now())
                .build();
    }
}