package com.luna.deepluna.repository;

import com.luna.deepluna.entity.ClarifyHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 澄清历史Repository
 */
@Repository
public interface ClarifyHistoryRepository extends JpaRepository<ClarifyHistory, String> {
    
    /**
     * 根据会话ID查找澄清历史
     */
    List<ClarifyHistory> findBySessionId(String sessionId);
    
    /**
     * 根据会话ID和轮次查找澄清历史
     */
    List<ClarifyHistory> findBySessionIdAndRoundNumber(String sessionId, Integer roundNumber);
    
    /**
     * 根据会话ID查找澄清历史，按轮次排序
     */
    List<ClarifyHistory> findBySessionIdOrderByRoundNumber(String sessionId);
}