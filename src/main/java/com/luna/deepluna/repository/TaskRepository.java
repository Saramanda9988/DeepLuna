package com.luna.deepluna.repository;

import com.luna.deepluna.entity.Task;
import com.luna.deepluna.common.enums.TaskStatus;
import com.luna.deepluna.common.enums.AgentTypeEnums;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 任务Repository
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, String> {
    
    /**
     * 根据会话ID查找任务
     */
    List<Task> findBySessionId(String sessionId);
    
    /**
     * 根据会话ID和状态查找任务
     */
    List<Task> findBySessionIdAndStatus(String sessionId, TaskStatus status);
    
    /**
     * 根据会话ID和代理类型查找任务
     */
    List<Task> findBySessionIdAndAgentType(String sessionId, AgentTypeEnums agentType);
}