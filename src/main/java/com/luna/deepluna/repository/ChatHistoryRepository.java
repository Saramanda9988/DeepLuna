package com.luna.deepluna.repository;

import com.luna.deepluna.dto.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 澄清历史Repository
 */
@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, String> {
    
    /**
     * 根据会话ID查找澄清历史
     */
    List<ChatHistory> findBySessionId(String sessionId);
    
    /**
     * 根据会话ID和轮次查找澄清历史
     */
    List<ChatHistory> findBySessionIdAndRoundNumber(String sessionId, Integer roundNumber);
    
    /**
     * 根据会话ID查找澄清历史，按轮次排序
     */
    List<ChatHistory> findBySessionIdOrderByRoundNumber(String sessionId);
    
    /**
     * 根据会话ID查找最大轮次
     */
    ChatHistory findTopBySessionIdOrderByRoundNumberDesc(String sessionId);
}