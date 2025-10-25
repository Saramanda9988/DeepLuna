package com.luna.deepluna.repository;

import com.luna.deepluna.dto.entity.Session;
import com.luna.deepluna.common.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 会话Repository
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, String> {
    
    /**
     * 根据用户ID查找会话
     */
    List<Session> findByUserId(Long userId);
    
    /**
     * 根据用户ID和状态查找会话
     */
    List<Session> findByUserIdAndStatus(Long userId, SessionStatus status);
}