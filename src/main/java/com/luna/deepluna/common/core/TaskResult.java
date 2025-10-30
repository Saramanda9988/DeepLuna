package com.luna.deepluna.common.core;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Base abstract class for all task results in the research system.
 * Contains common fields for task execution results and tracking.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class TaskResult {
    
    /**
     * Unique identifier for the research session
     */
    private String sessionId;
    
    /**
     * Unique identifier for this specific task
     */
    private String taskId;
    
    /**
     * Indicates whether the task execution was successful
     */
    private boolean success;
    
    /**
     * Error message if task execution failed
     */
    private String errorMessage;
    
    /**
     * Additional result data
     */
    private Map<String, Object> data = new HashMap<>();
    
    /**
     * Timestamp when the task was completed
     */
    private LocalDateTime completedAt;
    
    /**
     * Constructor for successful result
     */
    public TaskResult(String sessionId, String taskId, boolean success) {
        this.sessionId = sessionId;
        this.taskId = taskId;
        this.success = success;
        this.data = new HashMap<>();
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Add result data
     */
    public void addData(String key, Object value) {
        if (this.data == null) {
            this.data = new HashMap<>();
        }
        this.data.put(key, value);
    }
    
    /**
     * Get result data
     */
    public Object getData(String key) {
        return this.data != null ? this.data.get(key) : null;
    }
    
    /**
     * Create a successful result
     */
    public static <T extends TaskResult> T success(String sessionId, String taskId, T result) {
        result.setSessionId(sessionId);
        result.setTaskId(taskId);
        result.setSuccess(true);
        result.setCompletedAt(LocalDateTime.now());
        return result;
    }
    
    /**
     * Create a failed result
     */
    public static <T extends TaskResult> T failure(String sessionId, String taskId, String errorMessage, T result) {
        result.setSessionId(sessionId);
        result.setTaskId(taskId);
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setCompletedAt(LocalDateTime.now());
        return result;
    }
}